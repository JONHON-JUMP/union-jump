 ## Camstar - MES 人员数据库表

### 1. 人员表（EMPLOYEE）

| 序号 | 名称                             | 类型             | 可为空 | 默认 | 注释                                                    |
| :--- | :------------------------------- | :--------------- | :----- | :--- | :------------------------------------------------------ |
| 1    | `ALLOWOVERRIDESESSIONVALUES`     | `NUMBER(10)`     | Y      |      | 允许浏览器 Session 覆盖 ID                              |
| 2    | `CANLOGIN`                       | `NUMBER(10)`     | Y      |      | 是否可登录                                              |
| 3    | `CODEPOTYPE`                     | `NUMBER(10)`     | Y      |      | `CODEDEFINITION`关联字段                                |
| 4    | `CHANGECOUNT`                    | `NUMBER(10)`     | Y      |      | 更改次数                                                |
| 5    | `CHANGESTATUSUSID`               | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联字段，记录修改的记录（人，时间）    |
| 6    | `DESCRIPTION`                    | `VARCHAR2(255)`  | Y      |      | 字典描述                                                |
| 7    | `DOCMANAGERPASSWORD`             | `VARCHAR2(128)`  | Y      |      | 域名密码                                                |
| 8    | `DOCMANAGERUSER`                 | `VARCHAR2(255)`  | Y      |      | 域名号                                                  |
| 9    | `DOMAINNAME`                     | `VARCHAR2(30)`   | Y      |      | 域名                                                    |
| 10   | `EMAILADDRESS`                   | `VARCHAR2(255)`  | Y      |      | Email 地址                                              |
| 11   | `EMPLOYEEID`                     | `CHAR(16)`       | Y      |      | 人员 ID 主键                                            |
| 12   | `EMPLOYEEUSERNAME`               | `VARCHAR2(30)`   | Y      |      | 人员编码                                                |
| 13   | `ESCROLEGROUPID`                 | `CHAR(16)`       | Y      |      | 角色组 ID                                               |
| 14   | `FILTERTAGACCESS`                | `NUMBER(10)`     | Y      |      | 过滤器                                                  |
| 15   | `FILTERTAGS`                     | `CLOB`           | Y      |      | 过滤器标记                                              |
| 16   | `FILTERTAGSESSION`               | `CLOB`           | Y      |      | 过滤器 Session                                          |
| 17   | `FULLNAME`                       | `VARCHAR2(255)`  | Y      |      | 人员名称                                                |
| 18   | `HISTORYVIEWID`                  | `CHAR(16)`       | Y      |      | 历史记录                                                |
| 19   | `ICONID`                         | `NUMBER(10)`     | Y      |      | 图标 ID                                                 |
| 20   | `ISFROZEN`                       | `NUMBER(10)`     | Y      |      | 是否冻结                                                |
| 21   | `LANGUAGEACTIONDICTIONARYID`     | `CHAR(16)`       | Y      |      | 字典 ID                                                 |
| 22   | `MENUDEFINITIONID`               | `CHAR(16)`       | Y      |      | 菜单目录 ID                                             |
| 23   | `MODELERACCESS`                  | `NUMBER(10)`     | Y      |      | Model 操作权限                                          |
| 24   | `NOTES`                          | `VARCHAR2(2000)` | Y      |      | 注释 / 备注                                             |
| 25   | `PORTALMENUDEFINITIONID`         | `CHAR(16)`       | Y      |      | 菜单目录 ID                                             |
| 26   | `PORTALMOBILEMENUDEFINITIONID`   | `CHAR(16)`       | Y      |      | 子页面 ID                                               |
| 27   | `PRIMARYORGANIZATIONID`          | `CHAR(16)`       | Y      |      |                                                         |
| 28   | `SESSIONVALUESID`                | `CHAR(16)`       | Y      |      | **session 会话 ID（session 会话存有车间，工位等信息）** |
| 29   | `TERMINOLOGYDICTIONARYID`        | `CHAR(16)`       | Y      |      |                                                         |
| 30   | `TRAININGPLANID`                 | `CHAR(16)`       | Y      |      | 培训组 ID                                               |
| 31   | `UIPORTALPROFILEID`              | `CHAR(16)`       | Y      |      | 登录后要显示的页面 ID                                   |
| 32   | `USERCOMMENT`                    | `VARCHAR2(255)`  | Y      |      | 用户评议                                                |
| 33   | `USERPROFILEID`                  | `CHAR(16)`       | Y      |      | 用户预制表 ID（存储是否登陆显示车间、工位等信息的界面） |
| 34   | `WEBDWILLDOWNERMENUDEFINITIONID` | `CHAR(16)`       | Y      |      | 页面菜单 ID                                             |
| 35   | `WEBMENUDEFINITIONID`            | `CHAR(16)`       | Y      |      |                                                         |
| 36   | `CARDNO`                         | `VARCHAR2(30)`   | Y      |      | 员工卡号                                                |
| 37   | `PRODUCTLINEID`                  | `CHAR(16)`       | Y      |      | 流水线 ID                                               |
| 38   | `ERPNO`                          | `VARCHAR2(30)`   | Y      |      | ERP 号                                                  |
| 39   | `JOBNAME`                        | `VARCHAR2(200)`  | Y      |      | 所属岗位                                                |
| 40   | `PORTALHOMEPAGE`                 | `VARCHAR2(30)`   | Y      |      |                                                         |
| 41   | `WORKLICENSE`                    | `VARCHAR2(50)`   | Y      |      | 上岗证编号                                              |
| 42   | `WORKCENTERID`                   | `CHAR(16)`       | Y      |      |                                                         |
| 43   | `ISONDUTY`                       | `INTEGER`        | Y      | 1    | 是否在职                                                |

### 2. 班组人员关系表（TEAMEMPLOYEE）

| 序号 | 名称         | 类型         | 可为空 | 默认 | 注释   |
| :--- | :----------- | :----------- | :----- | :--- | :----- |
| 1    | `EMPLOYEEID` | `CHAR(16)`   | Y      |      | 人员ID |
| 2    | `FIELDID`    | `NUMBER(10)` | Y      |      |        |
| 3    | `SEQUENCE`   | `NUMBER(10)` | Y      |      | 顺序号 |
| 4    | `TEAMID`     | ``CHAR(16)`` | Y      |      | 班组ID |

### 3. 班组定义表（TEAM）

| 序号 | 名称                  | 类型             | 可为空 | 默认 | 注释                                     |
| :--- | :-------------------- | :--------------- | :----- | :--- | :--------------------------------------- |
| 1    | `CDOTYPEID`           | `NUMBER(10)`     | Y      |      | `CODEDEFINITION`关联字段                 |
| 2    | `CHANGECOUNT`         | `NUMBER(10)`     | Y      |      | 更改次数                                 |
| 3    | `CHANGEHISTORYID`     | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联 ID                  |
| 4    | `DESCRIPTION`         | `VARCHAR2(255)`  | Y      |      | 字典描述                                 |
| 5    | `FILTERTAGS`          | `CLOB`           | Y      |      | 过滤器标记                               |
| 6    | `ICONID`              | `NUMBER(10)`     | Y      |      | 图标 ID                                  |
| 7    | `ISFROZEN`            | `NUMBER(10)`     | Y      |      | 是否冻结                                 |
| 8    | `NOTES`               | `VARCHAR2(2000)` | Y      |      | 注释 / 备注                              |
| 9    | `TEAMID`              | `CHAR(16)`       | Y      |      | 班组 ID                                  |
| 10   | `TEAMNAME`            | `VARCHAR2(30)`   | Y      |      | 班组名称                                 |
| 11   | `FACTORYID`           | `CHAR(16)`       | Y      |      | 班组对应分厂 ID（FACTORY）               |
| 12   | `CHECKERID`           | `CHAR(16)`       | Y      |      | 班组检验员 ID（EMPLOYEE）（弃用 多对多） |
| 13   | `DISPATCHERID`        | `CHAR(16)`       | Y      |      | 班组调度员 ID（EMPLOYEE）（弃用 多对多） |
| 14   | `PRODUCTLINEID`       | `CHAR(16)`       | Y      |      | 流水线 ID（弃用）                        |
| 15   | `PROCESSABILITY`      | `VARCHAR2(30)`   | Y      |      | 班组能力                                 |
| 16   | `DISPATCHER_ERP_CODE` | `VARCHAR2(50)`   | Y      |      | 调度员 ERP 编码                          |
| 17   | `TEAMLEADER_CARD_NO`  | `VARCHAR2(50)`   | Y      |      | 班组长卡号                               |
| 18   | `DISPATCHER_NAME`     | `VARCHAR2(20)`   | Y      |      | 调度员姓名                               |
| 19   | `TEAMLEADER_NAME`     | `VARCHAR2(20)`   | Y      |      | 班组长姓名                               |
| 20   | `MONITORID`           | `CHAR(16)`       | Y      |      |                                          |

### 4. 车间定义表（FACTORY）

| 序号 | 名称                        | 类型             | 可为空 | 默认 | 注释                     |
| :--- | :-------------------------- | :--------------- | :----- | :--- | :----------------------- |
| 1    | `CDOTYPEID`                 | `NUMBER(10)`     | Y      |      | `CDODEFINITION`关联字段  |
| 2    | `CHANGECOUNT`               | `NUMBER(10)`     | Y      |      | 更改次数                 |
| 3    | `CHANGEMGTAPPLICATIONID`    | `CHAR(16)`       | Y      |      | 修改应用 ID              |
| 4    | `CHANGESTATUSID`            | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联 ID  |
| 5    | `CONTAINERNUMBERINGRULEID`  | `CHAR(16)`       | Y      |      | 工单创建规则 ID          |
| 6    | `DESCRIPTION`               | `VARCHAR2(255)`  | Y      |      | 字典描述                 |
| 7    | `DISPATCHRULEID`            | `CHAR(16)`       | Y      |      | 派工规则 ID              |
| 8    | `DISPLAYGENERALMESSAGE`     | `NUMBER(10)`     | Y      |      | 展示的常规消息           |
| 9    | `ENTERPRISEID`              | `CHAR(16)`       | Y      |      | 企业 ID                  |
| 10   | `FACTORYID`                 | `CHAR(16)`       | Y      |      | 车间 ID                  |
| 11   | `FACTORYNAME`               | `VARCHAR2(30)`   | Y      |      | 车间编码                 |
| 12   | `FILTERTAGS`                | `CLOB`           | Y      |      | 过滤器标记               |
| 13   | `GENERALMESSAGE`            | `VARCHAR2(4000)` | Y      |      | 常规消息                 |
| 14   | `ICONID`                    | `NUMBER(10)`     | Y      |      | 图标 ID                  |
| 15   | `ISFROZEN`                  | `NUMBER(10)`     | Y      |      | 是否冻结                 |
| 16   | `MAXTIMESINCEMSGSRETRIEVED` | `NUMBER`         | Y      |      |                          |
| 17   | `MFGCALENDARID`             | `CHAR(16)`       | Y      |      | MFG 日历 ID              |
| 18   | `NOTES`                     | `VARCHAR2(2000)` | Y      |      | 注释 / 备注              |
| 19   | `ORGANIZATIONID`            | `CHAR(16)`       | Y      |      | 组织                     |
| 20   | `PRINTQUEUEID`              | `CHAR(16)`       | Y      |      | 打印队列 ID              |
| 21   | `REPORTHEADING`             | `VARCHAR2(255)`  | Y      |      | 报告标题                 |
| 22   | `REQUIRELOCATION`           | `NUMBER(10)`     | Y      |      | 指定位置                 |
| 23   | `RETENTIONDAYS`             | `NUMBER(10)`     | Y      |      | 保留时间                 |
| 24   | `SMTPTRANSPORTID`           | `CHAR(16)`       | Y      |      | SMTP 简单邮件传输协议 ID |
| 25   | `TRAININGRQGROUPID`         | `CHAR(16)`       | Y      |      | 培训组 ID                |
| 26   | `WIPMSGDEFMGRID`            | `CHAR(16)`       | Y      |      |                          |
| 27   | `DEPARTMENTID`              | `CHAR(16)`       | Y      |      | 部门 ID                  |
| 28   | `RESOURCESPOTCHECKRESULTID` | `CHAR(16)`       | Y      |      | 设备点检结果 ID          |

### 5. 产线定义表（PRODUCTLINE）

| 序号 | 名称                   | 类型             | 可为空 | 默认 | 注释                    |
| :--- | :--------------------- | :--------------- | :----- | :--- | :---------------------- |
| 1    | `CDOTYPEID`            | `NUMBER(10)`     | Y      |      | `CDODEFINITION`关联字段 |
| 2    | `CHANGECOUNT`          | `NUMBER(10)`     | Y      |      | 更改次数                |
| 3    | `CHANGEHISTORYID`      | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联 ID |
| 4    | `DESCRIPTION`          | `VARCHAR2(255)`  | Y      |      | 字典描述                |
| 5    | `FACTORYID`            | `CHAR(16)`       | Y      |      | 车间 ID                 |
| 6    | `FILTERTAGS`           | `VARCHAR2(255)`  | Y      |      | 过滤器标记              |
| 7    | `ICONID`               | `NUMBER(10)`     | Y      |      | 图标 ID                 |
| 8    | `ISFROZEN`             | `NUMBER(10)`     | Y      |      | 是否冻结                |
| 9    | `NOTES`                | `VARCHAR2(2000)` | Y      |      | 注释 / 备注             |
| 10   | `PRODUCTLINEID`        | `CHAR(16)`       | Y      |      | 产线 ID                 |
| 11   | `PRODUCTLINENAME`      | `VARCHAR2(255)`  | Y      |      | 产线编码                |
| 12   | `TEAMID`               | `CHAR(16)`       | Y      |      | 班组 ID                 |
| 13   | `PRODUCTLINECAPACITY`  | `NUMBER`         | Y      |      | 产线容量                |
| 14   | `LINEHEADID`           | `CHAR(16)`       | Y      |      | 线头 ID                 |
| 15   | `MONITORCAPACITY`      | `NUMBER`         | Y      |      | 监控容量                |
| 16   | `LINETYPE`             | `VARCHAR2(30)`   | Y      |      | 产线类型                |
| 17   | `LOCATION`             | `VARCHAR2(30)`   | Y      |      | 产线位置                |
| 18   | `COLUMN671089157`      | `NUMBER`         | Y      |      |                         |
| 19   | `MONITIRCAPACITY`      | `NUMBER`         | Y      |      |                         |
| 20   | `SENDJOB_TYPE`         | `NVARCHAR2(200)` | Y      |      | 派工方式                |
| 21   | `LINE_STATUS`          | `NVARCHAR2(200)` | Y      |      | 产线状态                |
| 22   | `DISPATCHER_USER_CODE` | `NVARCHAR2(200)` | Y      |      | 调度员编码              |
| 23   | `PROCESSOR_USER_CODE`  | `NVARCHAR2(200)` | Y      |      | 工艺员编码              |
| 24   | `INSPECTOR_USER_CODE`  | `NVARCHAR2(200)` | Y      |      | 检验员编码              |
| 25   | `QE_USER_CODE`         | `NVARCHAR2(200)` | Y      |      | 质量工程师编码          |
| 26   | `LINE_HEAD_LOCATION`   | `NVARCHAR2(200)` | Y      |      | 线头区域                |
| 27   | `LINE_TAIL_LOCATION`   | `NVARCHAR2(200)` | Y      |      | 线尾区域                |
| 28   | `IS_AUTO_CALLMATERIAL` | `NVARCHAR2(200)` | Y      |      | 是否自动叫料            |
| 29   | `DAY_START_TIME`       | `NVARCHAR2(200)` | Y      |      | 白班开始时间            |
| 30   | `DAY_END_TIME`         | `NVARCHAR2(200)` | Y      |      | 白班结束时间            |
| 31   | `NIGHT_START_TIME`     | `NVARCHAR2(200)` | Y      |      | 夜班开始时间            |
| 32   | `MIGHT_END_TIME`       | `NVARCHAR2(200)` | Y      |      | 夜班结束时间            |
| 33   | `OUT_FLOOR_NO`         | `NVARCHAR2(200)` | Y      |      | 出库楼层                |
| 34   | `OUT_REQUIRE_CLEAR`    | `NVARCHAR2(200)` | Y      |      | 是否有清洁度要求        |
| 35   | `IS_USE`               | `NUMBER`         | Y      | 1    | 是否使用                |
| 36   | `COEFFICIENT`          | `NUMBER`         | Y      |      | 产线效率                |
| 37   | `AREA`                 | `NUMBER`         | Y      |      | 面积                    |

### 6. 部门-车间关系表（DEPT_WORKSHOP）

| 序号 | 名称                     | 类型             | 可为空 | 默认         | 注释                 |
| :--- | :----------------------- | :--------------- | :----- | :----------- | :------------------- |
| 1    | `WORKSHOP_CODE`          | `NVARCHAR2(200)` |        |              | 车间编码             |
| 2    | `DEPT_ID`                | `NVARCHAR2(200)` | Y      | `sys_guid()` | 部门编码             |
| 3    | `DEPT_NAME`              | `NVARCHAR2(200)` | Y      |              | 部门名称             |
| 4    | `IS_UNION_BY_DEPT`       | `NVARCHAR2(200)` | Y      | `'否'`       | 是否根据部门合并查询 |
| 5    | `ID`                     | `NVARCHAR2(200)` | Y      | `sys_guid()` | id                   |
| 6    | `TEAM_LINE_HAS_RELATION` | `NVARCHAR2(200)` | Y      | `'是'`       | 班组产线是否有关系   |

### 7. 角色定义表（ROLEDEF）

| 序号 | 名称              | 类型             | 可为空 | 默认 | 注释                    |
| :--- | :---------------- | :--------------- | :----- | :--- | :---------------------- |
| 1    | `CDOTYPEID`       | `NUMBER(10)`     | Y      |      | `CDODEFINITION`关联字段 |
| 2    | `CHANGECOUNT`     | `NUMBER(10)`     | Y      |      | 更改次数                |
| 3    | `CHANGEHISTORYID` | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联 ID |
| 4    | `DESCRIPTION`     | `VARCHAR2(255)`  | Y      |      | 字典描述                |
| 5    | `FILTERTAGS`      | `CLOB`           | Y      |      | 过滤器标记              |
| 6    | `ICONID`          | `NUMBER(10)`     | Y      |      | 图标 ID                 |
| 7    | `ISFROZEN`        | `NUMBER(10)`     | Y      |      | 是否冻结                |
| 8    | `NOTES`           | `VARCHAR2(2000)` | Y      |      | 注释 / 备注             |
| 9    | `ROLEID`          | `CHAR(16)`       | Y      |      | 角色 ID                 |
| 10   | `ROLENAME`        | `VARCHAR2(30)`   | Y      |      | 角色编码                |
| 11   | `ROLETYPE`        | `NUMBER(10)`     | Y      |      | 角色类型                |

### 8.人员角色对应信息表（EMPLOYEEROLE）

| 序号 | 名称                   | 类型           | 可为空 | 默认 | 注释                    |
| :--- | :--------------------- | :------------- | :----- | :--- | :---------------------- |
| 1    | `CDOTYPEID`            | `NUMBER(10)`   | Y      |      | `CDODEFINITION`关联字段 |
| 2    | `CHANGECOUNT`          | `NUMBER(10)`   | Y      |      | 更改次数                |
| 3    | `EMPLOYEEID`           | `CHAR(16)`     | Y      |      | 人员 ID                 |
| 4    | `EMPLOYEEROLEID`       | `CHAR(16)`     | Y      |      | 主键                    |
| 5    | `EXPORTIMPORTKEY`      | `VARCHAR2(36)` | Y      |      | 导出导入 Key            |
| 6    | `ISFROZEN`             | `NUMBER(10)`   | Y      |      | 是否冻结                |
| 7    | `ORGANIZATIONID`       | `CHAR(16)`     | Y      |      | 组织 ID                 |
| 8    | `PROPAGATETOCHILDORGS` | `NUMBER(10)`   | Y      |      |                         |
| 9    | `ROLEID`               | `CHAR(16)`     | Y      |      | 角色 ID                 |

### 9. 部门表（DEPARTMENT）

| 序号 | 名称                | 类型             | 可为空 | 默认 | 注释                    |
| :--- | :------------------ | :--------------- | :----- | :--- | :---------------------- |
| 1    | `CDOTYPEID`         | `NUMBER(10)`     | Y      |      | `CDODEFINITION`关联字段 |
| 2    | `CHANGECOUNT`       | `NUMBER(10)`     | Y      |      | 更改次数                |
| 3    | `CHANGEHISTORYID`   | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联 ID |
| 4    | `DEPARTMENTID`      | `CHAR(16)`       | Y      |      | 部门 ID                 |
| 5    | `DEPARTMENTNAME`    | `VARCHAR2(30)`   | Y      |      | 部门名称                |
| 6    | `DESCRIPTION`       | `VARCHAR2(255)`  | Y      |      | 字典描述                |
| 7    | `ENTERPRISEID`      | `CHAR(16)`       | Y      |      | 企业 ID                 |
| 8    | `FILTERTAGS`        | `VARCHAR2(255)`  | Y      |      | 过滤器标记              |
| 9    | `ICONID`            | `NUMBER(10)`     | Y      |      | 图标 ID                 |
| 10   | `ISFROZEN`          | `NUMBER(10)`     | Y      |      | 是否冻结                |
| 11   | `NOTES`             | `VARCHAR2(2000)` | Y      |      | 注释 / 备注             |
| 12   | `PDMDEPARTMENDCODE` | `VARCHAR2(30)`   | Y      |      | PDM 部门                |
| 13   | `OA_NO`             | `VARCHAR2(255)`  | Y      |      | OA 号                   |
| 14   | `TEST`              | `VARCHAR2(255)`  | Y      |      | 测试字段                |

### 10. **工位定义表** （WORKCENTER）

| 序号 | 名称                 | 类型             | 可为空 | 默认 | 注释                                                         |
| :--- | :------------------- | :--------------- | :----- | :--- | :----------------------------------------------------------- |
| 1    | `CDOTYPEID`          | `NUMBER(10)`     | Y      |      | `CDODEFINITION`关联字段                                      |
| 2    | `CHANGECOUNT`        | `NUMBER(10)`     | Y      |      | 更改次数                                                     |
| 3    | `CHANGESTATUSID`     | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联 ID                                      |
| 4    | `DESCRIPTION`        | `VARCHAR2(255)`  | Y      |      | 字典描述                                                     |
| 5    | `DISPATCHMETHOD`     | `NUMBER(10)`     | Y      |      | 派工方法                                                     |
| 6    | `DISPATCHRULEID`     | `CHAR(16)`       | Y      |      | 派工规则 ID                                                  |
| 7    | `FASTQUEUETIME`      | `NUMBER`         | Y      |      | 最快排队时间                                                 |
| 8    | `FILTERTAGS`         | `CLOB`           | Y      |      | 过滤器标记                                                   |
| 9    | `ICONID`             | `NUMBER(10)`     | Y      |      | 图标 ID                                                      |
| 10   | `IMAGE`              | `VARCHAR2(512)`  | Y      |      | 图像                                                         |
| 11   | `ISFROZEN`           | `NUMBER(10)`     | Y      |      | 是否冻结                                                     |
| 12   | `MFGCALENDARID`      | `CHAR(16)`       | Y      |      | MFG 日历 ID                                                  |
| 13   | `NORMALQUEUETIME`    | `NUMBER`         | Y      |      | 正常排队时间                                                 |
| 14   | `NOTES`              | `VARCHAR2(2000)` | Y      |      | 注释 / 备注                                                  |
| 15   | `RESOURCEGROUPID`    | `CHAR(16)`       | Y      |      | 大工序标签                                                   |
| 16   | `TRAININGREQGROUPID` | `CHAR(16)`       | Y      |      | 资源组 ID                                                    |
| 17   | `WIPMSGDEFMGRID`     | `CHAR(16)`       | Y      |      | 培训组 ID                                                    |
| 18   | `WORKCENTERID`       | `CHAR(16)`       | Y      |      | 工位 ID                                                      |
| 19   | `WORKCENTERNAME`     | `VARCHAR2(30)`   | Y      |      | 工位编码                                                     |
| 20   | `WORKSCHEDULEID`     | `CHAR(16)`       | Y      |      | 排程 ID                                                      |
| 21   | `PRODUCTLINEID`      | `CHAR(16)`       | Y      |      | 流水线 ID                                                    |
| 22   | `ZSENDADDRID`        | `CHAR(16)`       | Y      |      | 工位对应的 RFID（ZSENDADDR 表）                              |
| 23   | `PROCESSABILITYID`   | `CHAR(16)`       | Y      |      | 工位能力 ID                                                  |
| 24   | `TEAMID`             | `CHAR(16)`       | Y      |      | 班组 ID                                                      |
| 25   | `WORKCENTERLEVEL`    | `NUMBER(10)`     | Y      |      | 等级 0:JI 1:JII 2:JHT 3:JZ                                   |
| 26   | `WORKCENTERSTATUS`   | `NUMBER(10)`     | Y      | 1    | 状态 1: 启用 2: 禁用（控制是否派工）                         |
| 27   | `ISFIRSTWORKCENTER`  | `NUMBER(10)`     | Y      |      | 是否该线的第一个工位                                         |
| 28   | `BATCHNUM`           | `NUMBER(10)`     | Y      |      | 批次数量                                                     |
| 29   | `PRODUCTNUM`         | `NUMBER(10)`     | Y      |      | 产品数量                                                     |
| 30   | `ACTUALBATCHNUM`     | `NUMBER(10)`     | Y      |      | 实际批数                                                     |
| 31   | `ACTUALNUM`          | `NUMBER(10)`     | Y      |      | 实际数量                                                     |
| 32   | `PENDINGBATCHNUM`    | `NUMBER(10)`     | Y      |      | 可叫料批数                                                   |
| 33   | `PENDINGNUM`         | `NUMBER(10)`     | Y      |      | 可叫料数量                                                   |
| 34   | `INCOMING`           | `NUMBER(10)`     | Y      | 1    | 工位状态: 0/1: 关闭 / 开启（控制是否来料）                   |
| 35   | `BACKSTORELIMIT`     | `NUMBER(10)`     | Y      | 0    | 回库状态: 0: 不限制入库 1: 限制入库 2: 班组长刷卡确认后，可回库 |
| 36   | `WORKCENTERTYPE`     | `CHAR(3)`        | Y      | 0    | 工位类型: 0: 工位；1: 缓存区；2: 立体库房                    |
| 37   | `TRANSFLAG`          | `CHAR(3)`        | Y      | 1    | 流出开关: 0 关闭，1 开启（控制工位是否可流转）               |
| 38   | `CALLINGFLAG`        | `CHAR(3)`        | Y      | 1    | 叫料开关：控制是否可叫料（目前理货使用）                     |

### 11. UI Portal 侧页面表（UIPORTALPROFILE）

| 序号 | 名称                     | 类型            | 可为空 | 默认 | 注释                    |
| :--- | :----------------------- | :-------------- | :----- | :--- | :---------------------- |
| 1    | `CDOTYPEID`              | `NUMBER(10)`    | Y      |      | `CDODEFINITION`关联字段 |
| 2    | `CHANGECOUNT`            | `NUMBER(10)`    | Y      |      | 更改次数                |
| 3    | `EXPORTIMPORTKEY`        | `VARCHAR2(36)`  | Y      |      | 导出导入 Key            |
| 4    | `ISFROZEN`               | `NUMBER(10)`    | Y      |      | 是否冻结                |
| 5    | `MASTERPAGE`             | `VARCHAR2(512)` | Y      |      | 主页面                  |
| 6    | `PARENTID`               | `CHAR(16)`      | Y      |      | 父 ID                   |
| 7    | `PORTALHOMEPAGEID`       | `CHAR(16)`      | Y      |      | Portal 主页 ID          |
| 8    | `PORTALMOBILEHOMEPAGEID` | `CHAR(16)`      | Y      |      | Portal 移动端主页 ID    |
| 9    | `THEME`                  | `VARCHAR2(30)`  | Y      |      | 主题                    |
| 10   | `UIPORTALPROFILEID`      | `CHAR(16)`      | Y      |      | UI 侧页面 ID            |

### 12. UI 虚拟页面信息表（UIVIRTUALPAGE）

| 序号 | 名称                         | 类型             | 可为空 | 默认 | 注释                    |
| :--- | :--------------------------- | :--------------- | :----- | :--- | :---------------------- |
| 1    | `CDOTYPEID`                  | `NUMBER(10)`     | Y      |      | `CDODEFINITION`关联字段 |
| 2    | `CHANGECOUNT`                | `NUMBER(10)`     | Y      |      | 更改次数                |
| 3    | `CHANGEHISTORYID`            | `CHAR(16)`       | Y      |      | `CHANGESTATUS`表关联 ID |
| 4    | `CODEBEHINDFILE`             | `VARCHAR2(255)`  | Y      |      | 引用代码文件            |
| 5    | `CREATEDBYID`                | `CHAR(16)`       | Y      |      | 创建自 ID               |
| 6    | `DESCRIPTION`                | `VARCHAR2(255)`  | Y      |      | 字典描述                |
| 7    | `DEVELOPERPERSONALIZATIONID` | `CHAR(16)`       | Y      |      | 开发者个性化 ID         |
| 8    | `EPROCENABLED`               | `NUMBER(10)`     | Y      |      |                         |
| 9    | `FILTERTAGS`                 | `CLOB`           | Y      |      | 过滤器标记              |
| 10   | `ICONID`                     | `NUMBER(10)`     | Y      |      | 图标 ID                 |
| 11   | `INCLUDESREPORTSORCHARTS`    | `NUMBER(10)`     | Y      |      | 包含响应或图表          |
| 12   | `ISFROZEN`                   | `NUMBER(10)`     | Y      |      | 是否冻结                |
| 13   | `NOTES`                      | `VARCHAR2(2000)` | Y      |      | 注释 / 备注             |
| 14   | `SUBMENUNAME`                | `VARCHAR2(50)`   | Y      |      | 子菜单名                |
| 15   | `TEMPLATEPAGE`               | `VARCHAR2(512)`  | Y      |      | 模板页                  |
| 16   | `UIVIRTUALPAGEID`            | `CHAR(16)`       | Y      |      | 虚拟页 ID               |
| 17   | `UIVIRTUALPAGENAME`          | `VARCHAR2(255)`  | Y      |      | 虚拟页名                |

### 13. 人员信息缓存表（SESSIONVALUES）

| 序号 | 名称               | 类型           | 可为空 | 默认 | 注释                    |
| :--- | :----------------- | :------------- | :----- | :--- | :---------------------- |
| 1    | `APPLICATION`      | `NUMBER(10)`   | Y      |      | 应用                    |
| 2    | `CDOTYPEID`        | `NUMBER(10)`   | Y      |      | `CDODEFINITION`关联字段 |
| 3    | `CHANGECOUNT`      | `NUMBER(10)`   | Y      |      | 更改次数                |
| 4    | `CLIENT`           | `NUMBER(10)`   | Y      |      | 客户端                  |
| 5    | `EMPLOYEEID`       | `CHAR(16)`     | Y      |      | 人员编码                |
| 6    | `ENTERPRISEICONID` | `NUMBER(10)`   | Y      |      | 企业图标 ID             |
| 7    | `EXPORTIMPORTKEY`  | `VARCHAR2(36)` | Y      |      | 导出导入 Key            |
| 8    | `FACTORYICONID`    | `NUMBER(10)`   | Y      |      | 车间图标 ID             |
| 9    | `FACTORYID`        | `CHAR(16)`     | Y      |      | 车间 ID                 |
| 10   | `ISFROZEN`         | `NUMBER(10)`   | Y      |      | 是否冻结                |
| 11   | `LOCATIONICONID`   | `NUMBER(10)`   | Y      |      | 位置 ID                 |
| 12   | `OPERATIONID`      | `CHAR(16)`     | Y      |      | 工作中心 ID             |
| 13   | `RESOURCEID`       | `CHAR(16)`     | Y      |      | 设备 ID                 |
| 14   | `SERVICETYPEID`    | `NUMBER(10)`   | Y      |      | 服务类型 ID             |
| 15   | `SESSIONVALUESID`  | `CHAR(16)`     | Y      |      | 主键                    |
| 16   | `WORKCENTERICONID` | `NUMBER(10)`   | Y      |      | 工位图标 ID             |
| 17   | `WORKCENTERID`     | `CHAR(16)`     | Y      |      | 工位 ID                 |
| 18   | `WORKSTATIONID`    | `CHAR(16)`     | Y      |      | 工作站图标 ID           |