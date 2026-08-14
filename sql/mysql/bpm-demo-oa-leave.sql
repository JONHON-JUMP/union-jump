-- =============================================================================
-- BPM 示例流程：OA 请假（oa_leave）
-- 说明：业务表 + 配套字典/菜单已在 ruoyi-vue-pro.sql 中，本脚本主要补业务表。
-- 流程图文件：sql/bpm/oa_leave.bpmn20.xml（在「流程模型」中导入并发布）
-- =============================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 业务表：请假申请
-- ----------------------------
CREATE TABLE IF NOT EXISTS `bpm_oa_leave` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '请假表单主键',
  `user_id`              bigint       NOT NULL                COMMENT '申请人的用户编号',
  `type`                 tinyint      NOT NULL                COMMENT '请假类型，参见 bpm_oa_leave_type 字典',
  `reason`               varchar(512) NOT NULL DEFAULT ''     COMMENT '请假原因',
  `start_time`           datetime     NOT NULL                COMMENT '开始时间',
  `end_time`             datetime     NOT NULL                COMMENT '结束时间',
  `day`                  bigint       NOT NULL                COMMENT '请假天数',
  `status`               tinyint      NOT NULL                COMMENT '审批状态，参见 bpm_process_instance_status 字典',
  `process_instance_id`  varchar(64)           DEFAULT NULL   COMMENT '流程实例编号',
  `creator`              varchar(64)           DEFAULT ''     COMMENT '创建者',
  `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)           DEFAULT ''     COMMENT '更新者',
  `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)       NOT NULL DEFAULT b'0'  COMMENT '是否删除',
  `tenant_id`            bigint       NOT NULL DEFAULT 0       COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_bpm_oa_leave_user_id` (`user_id`) USING BTREE,
  KEY `idx_bpm_oa_leave_process_instance_id` (`process_instance_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 请假申请表';

-- ----------------------------
-- 2. 默认流程分类（若尚未创建，可执行；code 需与流程模型中的 category 一致）
-- ----------------------------
INSERT INTO `bpm_category` (`name`, `code`, `description`, `status`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'OA 示例', 'oa', 'OA 请假等示例流程', 0, 1, '1', NOW(), '1', NOW(), b'0', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `bpm_category` WHERE `code` = 'oa' AND `deleted` = b'0');

-- ----------------------------
-- 3. 若库中尚无 OA 请假菜单，可执行以下语句（一般 ruoyi-vue-pro.sql 已包含，可跳过）
-- 菜单路径：工作流程 -> OA 示例 -> 请假查询  (/bpm/oa/leave)
-- ----------------------------
