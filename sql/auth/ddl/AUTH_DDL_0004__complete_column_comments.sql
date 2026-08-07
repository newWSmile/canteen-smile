-- script_id: AUTH_DDL_0004
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0003
-- transactional: YES
-- purpose: 补齐现有 Auth 表全部字段的中文用途说明
-- rollback: 注释属于元数据说明，不执行回滚；后续修正只能新增更高编号脚本

SET search_path TO public;

BEGIN;

-- 已执行的历史脚本永久冻结；本脚本只补充字段注释，不改变表结构、约束、索引或业务数据。

-- auth_credential
COMMENT ON COLUMN public.auth_credential.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_credential.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_credential.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_credential.algorithm IS '密码哈希算法及参数标识';
COMMENT ON COLUMN public.auth_credential.password_changed_at IS '当前密码最近一次成功变更时间';
COMMENT ON COLUMN public.auth_credential.credential_version IS '账号认证凭证版本，变更后使旧认证状态失效';
COMMENT ON COLUMN public.auth_credential.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_credential.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_credential.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_credential.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_credential.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_credential.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_credential.version IS '并发更新控制使用的乐观锁版本';

-- auth_password_history
COMMENT ON COLUMN public.auth_password_history.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_password_history.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_password_history.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_password_history.password_hash IS '密码的自适应强哈希结果，禁止保存明文或可逆密文';
COMMENT ON COLUMN public.auth_password_history.algorithm IS '密码哈希算法及参数标识';
COMMENT ON COLUMN public.auth_password_history.changed_time IS '业务变更发生时间';
COMMENT ON COLUMN public.auth_password_history.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_password_history.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_password_history.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_password_history.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_password_history.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_password_history.version IS '并发更新控制使用的乐观锁版本';

-- auth_mobile_binding
COMMENT ON COLUMN public.auth_mobile_binding.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_mobile_binding.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_mobile_binding.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_mobile_binding.masked_mobile IS '供展示使用的脱敏手机号';
COMMENT ON COLUMN public.auth_mobile_binding.encryption_key_id IS '加密密文所使用的密钥版本标识';
COMMENT ON COLUMN public.auth_mobile_binding.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_mobile_binding.verified_time IS '手机号或安全信息通过二次验证的时间';
COMMENT ON COLUMN public.auth_mobile_binding.replaced_time IS '手机号绑定被新绑定关系替换的时间';
COMMENT ON COLUMN public.auth_mobile_binding.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_mobile_binding.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_mobile_binding.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_mobile_binding.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_mobile_binding.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_mobile_binding.version IS '并发更新控制使用的乐观锁版本';

-- auth_login_failure
COMMENT ON COLUMN public.auth_login_failure.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_login_failure.subject_key_hash IS '认证主体组合键的不可逆摘要';
COMMENT ON COLUMN public.auth_login_failure.last_ip_hash IS '最近一次认证失败来源 IP 摘要';
COMMENT ON COLUMN public.auth_login_failure.last_device_hash IS '最近一次失败登录所用设备标识摘要';
COMMENT ON COLUMN public.auth_login_failure.password_failures IS '当前连续密码校验失败次数';
COMMENT ON COLUMN public.auth_login_failure.captcha_required IS '后续密码登录是否必须先通过图形验证码';
COMMENT ON COLUMN public.auth_login_failure.locked_until IS '密码连续失败触发的临时锁定截止时间';
COMMENT ON COLUMN public.auth_login_failure.last_failure_time IS '最近一次认证失败时间';
COMMENT ON COLUMN public.auth_login_failure.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_login_failure.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_login_failure.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_login_failure.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_login_failure.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_login_failure.version IS '并发更新控制使用的乐观锁版本';

-- auth_sms_provider_config
COMMENT ON COLUMN public.auth_sms_provider_config.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_sms_provider_config.config_name IS '平台管理员识别配置所用的名称';
COMMENT ON COLUMN public.auth_sms_provider_config.provider_code IS '短信供应商类型编码';
COMMENT ON COLUMN public.auth_sms_provider_config.endpoint_url IS '短信供应商接口地址';
COMMENT ON COLUMN public.auth_sms_provider_config.encryption_key_id IS '加密密文所使用的密钥版本标识';
COMMENT ON COLUMN public.auth_sms_provider_config.credential_fingerprint IS '密钥密文的非敏感指纹，用于识别配置是否变化';
COMMENT ON COLUMN public.auth_sms_provider_config.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_sms_provider_config.is_default IS '当前配置是否为同类配置的默认项';
COMMENT ON COLUMN public.auth_sms_provider_config.last_verified_time IS '平台管理员最近一次验证配置可用性的时间';
COMMENT ON COLUMN public.auth_sms_provider_config.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_sms_provider_config.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_sms_provider_config.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_sms_provider_config.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_sms_provider_config.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_sms_provider_config.version IS '并发更新控制使用的乐观锁版本';

-- auth_security_link_config
COMMENT ON COLUMN public.auth_security_link_config.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_security_link_config.app_code IS '配置适用的前端应用编码';
COMMENT ON COLUMN public.auth_security_link_config.activation_base_url IS '账号激活链接允许使用的前端基础地址';
COMMENT ON COLUMN public.auth_security_link_config.password_reset_base_url IS '密码重置链接允许使用的前端基础地址';
COMMENT ON COLUMN public.auth_security_link_config.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_security_link_config.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_security_link_config.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_security_link_config.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_security_link_config.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_security_link_config.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_security_link_config.version IS '并发更新控制使用的乐观锁版本';

-- auth_sms_template_config
COMMENT ON COLUMN public.auth_sms_template_config.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_sms_template_config.provider_config_id IS '本次发送使用的短信供应商配置 ID';
COMMENT ON COLUMN public.auth_sms_template_config.purpose IS '验证码、短信或一次性票据的业务用途';
COMMENT ON COLUMN public.auth_sms_template_config.template_code IS '短信供应商分配的模板编码';
COMMENT ON COLUMN public.auth_sms_template_config.signature_name IS '短信供应商审核通过的签名名称';
COMMENT ON COLUMN public.auth_sms_template_config.parameter_schema IS '短信模板允许使用的参数结构定义';
COMMENT ON COLUMN public.auth_sms_template_config.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_sms_template_config.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_sms_template_config.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_sms_template_config.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_sms_template_config.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_sms_template_config.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_sms_template_config.version IS '并发更新控制使用的乐观锁版本';

-- auth_sms_challenge
COMMENT ON COLUMN public.auth_sms_challenge.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_sms_challenge.challenge_id IS '短信验证码挑战的外部唯一标识';
COMMENT ON COLUMN public.auth_sms_challenge.purpose IS '验证码、短信或一次性票据的业务用途';
COMMENT ON COLUMN public.auth_sms_challenge.mobile_hash IS '带服务端 Pepper 的手机号查询摘要';
COMMENT ON COLUMN public.auth_sms_challenge.code_hash IS '短信验证码或恢复码的不可逆摘要';
COMMENT ON COLUMN public.auth_sms_challenge.provider_config_id IS '本次发送使用的短信供应商配置 ID';
COMMENT ON COLUMN public.auth_sms_challenge.template_config_id IS '本次发送使用的短信模板配置 ID';
COMMENT ON COLUMN public.auth_sms_challenge.attempts IS '验证码累计校验失败次数';
COMMENT ON COLUMN public.auth_sms_challenge.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_sms_challenge.sent_time IS '短信提交供应商发送的时间';
COMMENT ON COLUMN public.auth_sms_challenge.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.auth_sms_challenge.consumed_time IS '一次性票据或凭证被成功使用的时间';
COMMENT ON COLUMN public.auth_sms_challenge.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_sms_challenge.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_sms_challenge.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_sms_challenge.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_sms_challenge.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_sms_challenge.version IS '并发更新控制使用的乐观锁版本';

-- auth_account_selector_ticket
COMMENT ON COLUMN public.auth_account_selector_ticket.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_account_selector_ticket.ticket_hash IS '一次性链接票据的不可逆摘要';
COMMENT ON COLUMN public.auth_account_selector_ticket.mobile_hash IS '带服务端 Pepper 的手机号查询摘要';
COMMENT ON COLUMN public.auth_account_selector_ticket.candidate_digest IS '手机号候选账号集合摘要，用于防止选择票据被篡改';
COMMENT ON COLUMN public.auth_account_selector_ticket.app_code IS '配置适用的前端应用编码';
COMMENT ON COLUMN public.auth_account_selector_ticket.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_account_selector_ticket.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.auth_account_selector_ticket.consumed_time IS '一次性票据或凭证被成功使用的时间';
COMMENT ON COLUMN public.auth_account_selector_ticket.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_account_selector_ticket.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_account_selector_ticket.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_account_selector_ticket.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_account_selector_ticket.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_account_selector_ticket.version IS '并发更新控制使用的乐观锁版本';

-- auth_activation_ticket
COMMENT ON COLUMN public.auth_activation_ticket.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_activation_ticket.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_activation_ticket.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_activation_ticket.ticket_hash IS '一次性链接票据的不可逆摘要';
COMMENT ON COLUMN public.auth_activation_ticket.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_activation_ticket.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.auth_activation_ticket.consumed_time IS '一次性票据或凭证被成功使用的时间';
COMMENT ON COLUMN public.auth_activation_ticket.superseded_time IS '一次性票据被新票据替代而失效的时间';
COMMENT ON COLUMN public.auth_activation_ticket.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_activation_ticket.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_activation_ticket.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_activation_ticket.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_activation_ticket.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_activation_ticket.version IS '并发更新控制使用的乐观锁版本';

-- auth_password_reset_ticket
COMMENT ON COLUMN public.auth_password_reset_ticket.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_password_reset_ticket.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_password_reset_ticket.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_password_reset_ticket.reset_mode IS '密码重置采用的验证和交付模式';
COMMENT ON COLUMN public.auth_password_reset_ticket.ticket_hash IS '一次性链接票据的不可逆摘要';
COMMENT ON COLUMN public.auth_password_reset_ticket.initiated_by_type IS '发起密码重置流程的身份类型';
COMMENT ON COLUMN public.auth_password_reset_ticket.initiated_by_id IS '发起密码重置流程的身份 ID';
COMMENT ON COLUMN public.auth_password_reset_ticket.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_password_reset_ticket.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.auth_password_reset_ticket.consumed_time IS '一次性票据或凭证被成功使用的时间';
COMMENT ON COLUMN public.auth_password_reset_ticket.superseded_time IS '一次性票据被新票据替代而失效的时间';
COMMENT ON COLUMN public.auth_password_reset_ticket.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_password_reset_ticket.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_password_reset_ticket.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_password_reset_ticket.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_password_reset_ticket.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_password_reset_ticket.version IS '并发更新控制使用的乐观锁版本';

-- auth_platform_recovery_code
COMMENT ON COLUMN public.auth_platform_recovery_code.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_platform_recovery_code.platform_identity_id IS '关联的独立平台管理身份 ID';
COMMENT ON COLUMN public.auth_platform_recovery_code.batch_id IS '恢复码所属生成批次 ID';
COMMENT ON COLUMN public.auth_platform_recovery_code.code_hash IS '短信验证码或恢复码的不可逆摘要';
COMMENT ON COLUMN public.auth_platform_recovery_code.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_platform_recovery_code.consumed_time IS '一次性票据或凭证被成功使用的时间';
COMMENT ON COLUMN public.auth_platform_recovery_code.superseded_time IS '一次性票据被新票据替代而失效的时间';
COMMENT ON COLUMN public.auth_platform_recovery_code.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_platform_recovery_code.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_platform_recovery_code.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_platform_recovery_code.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_platform_recovery_code.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_platform_recovery_code.version IS '并发更新控制使用的乐观锁版本';

-- auth_reauth_ticket
COMMENT ON COLUMN public.auth_reauth_ticket.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_reauth_ticket.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_reauth_ticket.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_reauth_ticket.ticket_hash IS '一次性链接票据的不可逆摘要';
COMMENT ON COLUMN public.auth_reauth_ticket.allowed_action IS '再认证票据允许执行的唯一敏感操作';
COMMENT ON COLUMN public.auth_reauth_ticket.verify_method IS '敏感操作再认证所采用的验证方式';
COMMENT ON COLUMN public.auth_reauth_ticket.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_reauth_ticket.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.auth_reauth_ticket.consumed_time IS '一次性票据或凭证被成功使用的时间';
COMMENT ON COLUMN public.auth_reauth_ticket.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_reauth_ticket.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_reauth_ticket.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_reauth_ticket.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_reauth_ticket.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_reauth_ticket.version IS '并发更新控制使用的乐观锁版本';

-- auth_device_session
COMMENT ON COLUMN public.auth_device_session.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_device_session.session_id IS '关联的设备会话 ID';
COMMENT ON COLUMN public.auth_device_session.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_device_session.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_device_session.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.auth_device_session.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.auth_device_session.app_code IS '配置适用的前端应用编码';
COMMENT ON COLUMN public.auth_device_session.device_id_hash IS '客户端设备稳定标识的不可逆摘要';
COMMENT ON COLUMN public.auth_device_session.device_type IS '设备类型编码';
COMMENT ON COLUMN public.auth_device_session.device_name IS '用户可识别的设备名称';
COMMENT ON COLUMN public.auth_device_session.login_method IS '建立会话所使用的登录方式';
COMMENT ON COLUMN public.auth_device_session.login_ip_masked IS '登录来源 IP 的脱敏展示值';
COMMENT ON COLUMN public.auth_device_session.login_time IS '设备会话建立时间';
COMMENT ON COLUMN public.auth_device_session.last_active_time IS '设备会话最近一次有效活动时间';
COMMENT ON COLUMN public.auth_device_session.idle_expires_at IS '会话因持续不活跃而失效的时间';
COMMENT ON COLUMN public.auth_device_session.absolute_expires_at IS '会话绝对失效时间，达到后不因活跃而续期';
COMMENT ON COLUMN public.auth_device_session.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_device_session.snapshot_version IS '当前会话权限快照版本号';
COMMENT ON COLUMN public.auth_device_session.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_device_session.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_device_session.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_device_session.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_device_session.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_device_session.version IS '并发更新控制使用的乐观锁版本';

-- auth_permission_snapshot
COMMENT ON COLUMN public.auth_permission_snapshot.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_permission_snapshot.session_id IS '关联的设备会话 ID';
COMMENT ON COLUMN public.auth_permission_snapshot.snapshot_version IS '当前会话权限快照版本号';
COMMENT ON COLUMN public.auth_permission_snapshot.payload_json IS '事件或权限快照的最小 JSON 载荷';
COMMENT ON COLUMN public.auth_permission_snapshot.signature IS '权限快照的 HMAC 完整性签名';
COMMENT ON COLUMN public.auth_permission_snapshot.signature_key_id IS '生成 HMAC 签名所用密钥版本标识';
COMMENT ON COLUMN public.auth_permission_snapshot.user_authz_version IS '权限快照生成时使用的账号授权版本';
COMMENT ON COLUMN public.auth_permission_snapshot.role_version_digest IS '权限快照包含的有效角色版本集合摘要';
COMMENT ON COLUMN public.auth_permission_snapshot.org_path_version IS '权限快照生成时使用的机构路径版本';
COMMENT ON COLUMN public.auth_permission_snapshot.tenant_security_version IS '权限快照生成时使用的租户安全版本';
COMMENT ON COLUMN public.auth_permission_snapshot.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_permission_snapshot.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.auth_permission_snapshot.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_permission_snapshot.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_permission_snapshot.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_permission_snapshot.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_permission_snapshot.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_permission_snapshot.version IS '并发更新控制使用的乐观锁版本';

-- auth_consumed_event
COMMENT ON COLUMN public.auth_consumed_event.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_consumed_event.event_id IS '跨服务事件的全局唯一 ID';
COMMENT ON COLUMN public.auth_consumed_event.event_type IS '跨服务事件类型编码';
COMMENT ON COLUMN public.auth_consumed_event.payload_digest IS '事件载荷的完整性摘要';
COMMENT ON COLUMN public.auth_consumed_event.result IS '审计事件或安全操作的执行结果';
COMMENT ON COLUMN public.auth_consumed_event.consumed_time IS '一次性票据或凭证被成功使用的时间';
COMMENT ON COLUMN public.auth_consumed_event.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_consumed_event.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_consumed_event.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_consumed_event.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_consumed_event.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_consumed_event.version IS '并发更新控制使用的乐观锁版本';

-- auth_audit_log
COMMENT ON COLUMN public.auth_audit_log.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_audit_log.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.auth_audit_log.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_audit_log.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_audit_log.operator_type IS '执行操作的身份类型';
COMMENT ON COLUMN public.auth_audit_log.operator_id IS '执行操作的平台身份或租户账号 ID';
COMMENT ON COLUMN public.auth_audit_log.action_code IS '审计或授权操作的动作编码';
COMMENT ON COLUMN public.auth_audit_log.result IS '审计事件或安全操作的执行结果';
COMMENT ON COLUMN public.auth_audit_log.login_method IS '建立会话所使用的登录方式';
COMMENT ON COLUMN public.auth_audit_log.failure_reason_code IS '认证或操作失败的原因编码';
COMMENT ON COLUMN public.auth_audit_log.ip_hash IS '访问来源 IP 的不可逆摘要';
COMMENT ON COLUMN public.auth_audit_log.device_summary IS '经过脱敏处理的设备信息摘要';
COMMENT ON COLUMN public.auth_audit_log.trace_id IS '关联请求的分布式链路追踪 ID';
COMMENT ON COLUMN public.auth_audit_log.occurred_time IS '安全或审计事件实际发生时间';
COMMENT ON COLUMN public.auth_audit_log.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_audit_log.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_audit_log.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_audit_log.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_audit_log.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_audit_log.version IS '并发更新控制使用的乐观锁版本';

-- auth_idempotency_record
COMMENT ON COLUMN public.auth_idempotency_record.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_idempotency_record.subject_type IS '认证主体类型，区分平台身份和租户账号';
COMMENT ON COLUMN public.auth_idempotency_record.subject_id IS '认证主体 ID，具体含义由主体类型决定';
COMMENT ON COLUMN public.auth_idempotency_record.operation_code IS '幂等记录对应的业务操作编码';
COMMENT ON COLUMN public.auth_idempotency_record.idempotency_key_hash IS '客户端幂等键的不可逆摘要';
COMMENT ON COLUMN public.auth_idempotency_record.request_hash IS '幂等请求关键参数的摘要';
COMMENT ON COLUMN public.auth_idempotency_record.response_reference IS '幂等操作结果的非敏感引用';
COMMENT ON COLUMN public.auth_idempotency_record.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.auth_idempotency_record.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.auth_idempotency_record.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.auth_idempotency_record.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_idempotency_record.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_idempotency_record.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_idempotency_record.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.auth_idempotency_record.version IS '并发更新控制使用的乐观锁版本';

COMMIT;

