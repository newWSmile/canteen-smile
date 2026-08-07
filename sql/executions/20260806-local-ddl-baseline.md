# LOCAL PostgreSQL DDL 执行基线

用户于 2026-08-06 确认以下 DDL 已全部在 LOCAL PostgreSQL 环境成功执行。原始执行起止时间未知，本记录不虚构时间、影响行数或连接信息；SHA-256 为确认后仓库脚本的冻结基线。

| 脚本 ID | 目标库 | 执行结果 | 文件 SHA-256 |
| --- | --- | --- | --- |
| `IAM_DDL_0001` | IAM | `SUCCESS` | `d2cef64f6d86c0dbe67db72ae3b7dbe99c11c4a59171bf323fa279902ee2eb1a` |
| `IAM_DDL_0002` | IAM | `SUCCESS` | `2e735da3414ff60c4ac6489be8a1f72d5fe10fcc8cd1684dfb18fe365874a3a4` |
| `IAM_DDL_0003` | IAM | `SUCCESS` | `1da8442792aa6b03806f922d61d22d8fb8dd2b432473f567cd0db9eae8bd5b6f` |
| `IAM_DDL_0004` | IAM | `SUCCESS` | `11a206455d80221590acb6944920b6a7f72b3114e9011cee2f880e76fdd960c1` |
| `IAM_DDL_0005` | IAM | `SUCCESS` | `24d87a5d0a91dfb2286dbc6605103fe5d194496425a458ac32b999b54fd2031e` |
| `IAM_DDL_0006` | IAM | `SUCCESS` | `48d466c60c86451f8f6fa3d685cb710f30dbbad7a36bf02657ec05b52e563746` |
| `AUTH_DDL_0001` | AUTH | `SUCCESS` | `b0c879e11605665bcffa8312f49b4c5af0eaf3ce1e450517929ed3ff39788491` |
| `AUTH_DDL_0002` | AUTH | `SUCCESS` | `c08dbb3e4d2fd0d968676967ee367b9a0dcdd9f92f8c33b7146c3593e4f469a4` |
| `AUTH_DDL_0003` | AUTH | `SUCCESS` | `54d18ff62162942aa180d113597b577c3cd9290c06efaa75f716ee6d709ad7b5` |
| `AUTH_DDL_0004` | AUTH | `SUCCESS` | `cef3a1c38b4cf2ba4451126d5debef30ef1484ec4e9c5bf6bfe35a6e9ac47a08` |

## 冻结规则

上述脚本自本记录建立后不得修改、覆盖、改名或删除。任何结构、约束、索引、注释修正必须新增更高编号 DDL；业务数据初始化或修正必须新增对应数据库的 DML。
