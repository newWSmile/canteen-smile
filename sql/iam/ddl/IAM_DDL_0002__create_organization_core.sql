-- script_id: IAM_DDL_0002
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0001
-- transactional: YES
-- purpose: 创建机构类型模板、租户机构类型、行政区域引用、机构树、闭包关系和永久编码记录
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.iam_org_type_template_seq AS bigint;
CREATE SEQUENCE public.iam_org_type_template_relation_seq AS bigint;
CREATE SEQUENCE public.iam_admin_region_seq AS bigint;
CREATE SEQUENCE public.iam_org_type_seq AS bigint;
CREATE SEQUENCE public.iam_org_type_relation_seq AS bigint;
CREATE SEQUENCE public.iam_organization_seq AS bigint;
CREATE SEQUENCE public.iam_organization_closure_seq AS bigint;
CREATE SEQUENCE public.iam_org_code_registry_seq AS bigint;
CREATE SEQUENCE public.iam_org_name_history_seq AS bigint;

CREATE TABLE public.iam_org_type_template (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_type_template_seq'::regclass),
    template_version bigint NOT NULL,
    type_code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    status varchar(32) NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_type_template PRIMARY KEY (id),
    CONSTRAINT uk_iam_org_type_template_version_code UNIQUE (template_version, type_code),
    CONSTRAINT ck_iam_org_type_template_value
        CHECK (
            template_version > 0
            AND btrim(type_code) <> ''
            AND btrim(name) <> ''
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_org_type_template_seq
    OWNED BY public.iam_org_type_template.id;

CREATE INDEX idx_iam_org_type_template_version_status
    ON public.iam_org_type_template (template_version, status, sort_order, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_org_type_template IS '平台维护并按版本复制到租户的机构类型模板';
COMMENT ON COLUMN public.iam_org_type_template.template_version IS '模板版本，同版本包含多种机构类型';
COMMENT ON COLUMN public.iam_org_type_template.type_code IS '模板版本内唯一的机构类型编码';
COMMENT ON COLUMN public.iam_org_type_template.status IS '模板类型状态，由 IAM 应用枚举管理';

CREATE TABLE public.iam_org_type_template_relation (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_type_template_relation_seq'::regclass),
    template_version bigint NOT NULL,
    parent_type_code varchar(64) NOT NULL,
    child_type_code varchar(64) NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_type_template_relation PRIMARY KEY (id),
    CONSTRAINT uk_iam_org_type_template_relation
        UNIQUE (template_version, parent_type_code, child_type_code),
    CONSTRAINT fk_iam_org_type_template_relation_parent
        FOREIGN KEY (template_version, parent_type_code)
        REFERENCES public.iam_org_type_template (template_version, type_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_type_template_relation_child
        FOREIGN KEY (template_version, child_type_code)
        REFERENCES public.iam_org_type_template (template_version, type_code)
        ON DELETE RESTRICT,
    CONSTRAINT ck_iam_org_type_template_relation_not_self
        CHECK (parent_type_code <> child_type_code),
    CONSTRAINT ck_iam_org_type_template_relation_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_org_type_template_relation_seq
    OWNED BY public.iam_org_type_template_relation.id;

CREATE INDEX idx_iam_org_type_template_relation_child
    ON public.iam_org_type_template_relation (template_version, child_type_code, parent_type_code)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_org_type_template_relation IS '平台机构类型模板允许的父子关系；发布前由 Service 校验为 DAG';

CREATE TABLE public.iam_admin_region (
    id bigint NOT NULL DEFAULT nextval('public.iam_admin_region_seq'::regclass),
    parent_id bigint,
    region_code varchar(32) NOT NULL,
    name varchar(128) NOT NULL,
    level_code varchar(32) NOT NULL,
    status varchar(32) NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_admin_region PRIMARY KEY (id),
    CONSTRAINT uk_iam_admin_region_code UNIQUE (region_code),
    CONSTRAINT fk_iam_admin_region_parent
        FOREIGN KEY (parent_id) REFERENCES public.iam_admin_region (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_admin_region_not_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_iam_admin_region_value
        CHECK (
            btrim(region_code) <> ''
            AND btrim(name) <> ''
            AND btrim(level_code) <> ''
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_admin_region_seq
    OWNED BY public.iam_admin_region.id;

CREATE INDEX idx_iam_admin_region_parent
    ON public.iam_admin_region (parent_id, status, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_admin_region IS '可选行政区域引用；不参与租户隔离或机构树父子校验';
COMMENT ON COLUMN public.iam_admin_region.parent_id IS '行政区域自身的上级区域，与机构 parent_id 无关';
COMMENT ON COLUMN public.iam_admin_region.region_code IS '行政区域稳定编码';

CREATE TABLE public.iam_org_type (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_type_seq'::regclass),
    tenant_id bigint NOT NULL,
    type_code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    status varchar(32) NOT NULL,
    source_template_version bigint,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_type PRIMARY KEY (id),
    CONSTRAINT uk_iam_org_type_tenant_code UNIQUE (tenant_id, type_code),
    CONSTRAINT uk_iam_org_type_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_iam_org_type_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_org_type_value
        CHECK (
            btrim(type_code) <> ''
            AND btrim(name) <> ''
            AND (source_template_version IS NULL OR source_template_version > 0)
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_org_type_seq
    OWNED BY public.iam_org_type.id;

CREATE INDEX idx_iam_org_type_tenant_status
    ON public.iam_org_type (tenant_id, status, sort_order, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_org_type IS '从平台模板复制后由单个租户独立维护的机构类型';
COMMENT ON COLUMN public.iam_org_type.tenant_id IS '机构类型所属租户';
COMMENT ON COLUMN public.iam_org_type.type_code IS '租户内永久唯一的机构类型编码';
COMMENT ON COLUMN public.iam_org_type.source_template_version IS '初始化来源模板版本，自定义类型可以为空';

CREATE TABLE public.iam_org_type_relation (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_type_relation_seq'::regclass),
    tenant_id bigint NOT NULL,
    parent_type_id bigint NOT NULL,
    child_type_id bigint NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_type_relation PRIMARY KEY (id),
    CONSTRAINT uk_iam_org_type_relation_types UNIQUE (tenant_id, parent_type_id, child_type_id),
    CONSTRAINT fk_iam_org_type_relation_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_type_relation_parent
        FOREIGN KEY (tenant_id, parent_type_id)
        REFERENCES public.iam_org_type (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_type_relation_child
        FOREIGN KEY (tenant_id, child_type_id)
        REFERENCES public.iam_org_type (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_org_type_relation_not_self CHECK (parent_type_id <> child_type_id),
    CONSTRAINT ck_iam_org_type_relation_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_org_type_relation_seq
    OWNED BY public.iam_org_type_relation.id;

CREATE INDEX idx_iam_org_type_relation_child
    ON public.iam_org_type_relation (tenant_id, child_type_id, parent_type_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_org_type_relation IS '租户允许的机构类型父子关系；Service 必须校验整图无环';

CREATE TABLE public.iam_organization (
    id bigint NOT NULL DEFAULT nextval('public.iam_organization_seq'::regclass),
    tenant_id bigint NOT NULL,
    parent_id bigint,
    org_type_id bigint NOT NULL,
    business_code varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    normalized_name varchar(200) NOT NULL,
    admin_region_id bigint,
    own_status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    path_version bigint NOT NULL DEFAULT 0,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_organization PRIMARY KEY (id),
    CONSTRAINT uk_iam_organization_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_iam_organization_business_code UNIQUE (tenant_id, business_code),
    CONSTRAINT fk_iam_organization_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_organization_parent
        FOREIGN KEY (tenant_id, parent_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_organization_type
        FOREIGN KEY (tenant_id, org_type_id)
        REFERENCES public.iam_org_type (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_organization_admin_region
        FOREIGN KEY (admin_region_id) REFERENCES public.iam_admin_region (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_organization_not_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_iam_organization_own_status CHECK (own_status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_iam_organization_value
        CHECK (
            btrim(business_code) <> ''
            AND btrim(name) <> ''
            AND btrim(normalized_name) <> ''
            AND path_version >= 0
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_organization_seq
    OWNED BY public.iam_organization.id;

CREATE UNIQUE INDEX uk_iam_organization_one_root
    ON public.iam_organization (tenant_id)
    WHERE parent_id IS NULL AND is_deleted = false;

CREATE UNIQUE INDEX uk_iam_organization_sibling_name
    ON public.iam_organization (tenant_id, parent_id, normalized_name)
    WHERE parent_id IS NOT NULL AND is_deleted = false;

CREATE INDEX idx_iam_organization_parent_status
    ON public.iam_organization (tenant_id, parent_id, own_status, id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_organization_type
    ON public.iam_organization (tenant_id, org_type_id, id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_organization_admin_region
    ON public.iam_organization (admin_region_id, id)
    WHERE admin_region_id IS NOT NULL AND is_deleted = false;

COMMENT ON TABLE public.iam_organization IS '租户内任意深度机构树节点';
COMMENT ON COLUMN public.iam_organization.parent_id IS '同租户父机构；根机构为空';
COMMENT ON COLUMN public.iam_organization.org_type_id IS '租户独立维护的机构类型';
COMMENT ON COLUMN public.iam_organization.business_code IS '租户内永久唯一且原则上不可修改的机构业务编码';
COMMENT ON COLUMN public.iam_organization.normalized_name IS '用于同一父机构下名称唯一校验的归一化名称';
COMMENT ON COLUMN public.iam_organization.admin_region_id IS '可选行政区域关联，不参与机构树关系';
COMMENT ON COLUMN public.iam_organization.own_status IS '机构自身状态；祖先停用产生的实际状态不写回本字段';
COMMENT ON COLUMN public.iam_organization.path_version IS '机构路径版本，迁移时提升并使相关授权快照失效';

CREATE TABLE public.iam_organization_closure (
    id bigint NOT NULL DEFAULT nextval('public.iam_organization_closure_seq'::regclass),
    tenant_id bigint NOT NULL,
    ancestor_id bigint NOT NULL,
    descendant_id bigint NOT NULL,
    depth integer NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_organization_closure PRIMARY KEY (id),
    CONSTRAINT fk_iam_organization_closure_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_organization_closure_ancestor
        FOREIGN KEY (tenant_id, ancestor_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_organization_closure_descendant
        FOREIGN KEY (tenant_id, descendant_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_organization_closure_depth
        CHECK (
            (depth = 0 AND ancestor_id = descendant_id)
            OR (depth > 0 AND ancestor_id <> descendant_id)
        ),
    CONSTRAINT ck_iam_organization_closure_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_organization_closure_seq
    OWNED BY public.iam_organization_closure.id;

CREATE UNIQUE INDEX uk_iam_organization_closure_path
    ON public.iam_organization_closure (tenant_id, ancestor_id, descendant_id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_organization_closure_ancestor
    ON public.iam_organization_closure (tenant_id, ancestor_id, depth, descendant_id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_organization_closure_descendant
    ON public.iam_organization_closure (tenant_id, descendant_id, depth, ancestor_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_organization_closure IS '机构祖先与后代闭包关系；每个机构必须包含 depth=0 的自身记录';
COMMENT ON COLUMN public.iam_organization_closure.depth IS '祖先到后代的层级距离，自身为 0';

CREATE TABLE public.iam_org_code_registry (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_code_registry_seq'::regclass),
    tenant_id bigint NOT NULL,
    normalized_code varchar(64) NOT NULL,
    organization_id bigint NOT NULL,
    reserved_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_code_registry PRIMARY KEY (id),
    CONSTRAINT uk_iam_org_code_registry_code UNIQUE (tenant_id, normalized_code),
    CONSTRAINT uk_iam_org_code_registry_org UNIQUE (tenant_id, organization_id),
    CONSTRAINT ck_iam_org_code_registry_value
        CHECK (btrim(normalized_code) <> '' AND organization_id > 0 AND version >= 0),
    CONSTRAINT ck_iam_org_code_registry_never_deleted CHECK (is_deleted = false)
);

ALTER SEQUENCE public.iam_org_code_registry_seq
    OWNED BY public.iam_org_code_registry.id;

COMMENT ON TABLE public.iam_org_code_registry IS '租户内永久保留所有使用过的机构业务编码';
COMMENT ON COLUMN public.iam_org_code_registry.tenant_id IS '首次占用编码的租户 ID，不设置外键以保留历史';
COMMENT ON COLUMN public.iam_org_code_registry.normalized_code IS '按服务端统一规则归一化的机构业务编码';
COMMENT ON COLUMN public.iam_org_code_registry.organization_id IS '首次占用该编码的机构 ID，不设置外键以保留历史';

CREATE TABLE public.iam_org_name_history (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_name_history_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    old_name varchar(200) NOT NULL,
    new_name varchar(200) NOT NULL,
    changed_by bigint NOT NULL,
    changed_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_name_history PRIMARY KEY (id),
    CONSTRAINT fk_iam_org_name_history_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_name_history_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_org_name_history_value
        CHECK (
            btrim(old_name) <> ''
            AND btrim(new_name) <> ''
            AND old_name <> new_name
            AND changed_by > 0
            AND is_deleted = false
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_org_name_history_seq
    OWNED BY public.iam_org_name_history.id;

CREATE INDEX idx_iam_org_name_history_org_time
    ON public.iam_org_name_history (tenant_id, organization_id, changed_time DESC, id DESC);

COMMENT ON TABLE public.iam_org_name_history IS '机构名称变更只追加历史';

ALTER TABLE public.iam_tenant
    ADD CONSTRAINT fk_iam_tenant_root_organization
    FOREIGN KEY (id, root_organization_id)
    REFERENCES public.iam_organization (tenant_id, id)
    ON DELETE RESTRICT;

COMMIT;
