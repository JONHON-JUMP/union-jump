package cn.jonhon.jump.module.system.convert.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.jonhon.jump.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.jonhon.jump.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthSmsLoginReqVO;
import cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthSmsSendReqVO;
import cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthSocialLoginReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.enums.permission.MenuTypeEnum;
import cn.jonhon.jump.module.system.util.MenuStyleHelper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.slf4j.LoggerFactory;

import java.util.*;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.filterList;
import static cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO.ID_ROOT;

@Mapper
public interface AuthConvert {

    AuthConvert INSTANCE = Mappers.getMapper(AuthConvert.class);

    default AuthPermissionInfoRespVO convert(AdminUserDO user, List<RoleDO> roleList, List<MenuDO> menuList,
                                             Map<Long, MenuColorDO> colorMap) {
        // 须在 buildMenuTree 之前提取权限：buildMenuTree 会移除按钮类型菜单
        Set<String> permissions = convertSet(menuList, MenuDO::getPermission);
        permissions.removeIf(StrUtil::isEmpty);
        List<AuthPermissionInfoRespVO.MenuVO> menus = buildMenuTree(new ArrayList<>(menuList));
        fillMenuColors(menus, colorMap);
        return AuthPermissionInfoRespVO.builder()
                .user(BeanUtils.toBean(user, AuthPermissionInfoRespVO.UserVO.class))
                .roles(convertSet(roleList, RoleDO::getCode))
                .permissions(permissions)
                .menus(menus)
                .build();
    }

    default AuthPermissionInfoRespVO convert(AdminUserDO user, List<RoleDO> roleList, List<MenuDO> menuList) {
        return convert(user, roleList, menuList, Collections.emptyMap());
    }

    /**
     * 将菜单列表，构建成菜单树
     *
     * @param menuList 菜单列表
     * @return 菜单树
     */
    default List<AuthPermissionInfoRespVO.MenuVO> buildMenuTree(List<MenuDO> menuList) {
        if (CollUtil.isEmpty(menuList)) {
            return Collections.emptyList();
        }
        // 移除按钮
        menuList.removeIf(menu -> menu.getType().equals(MenuTypeEnum.BUTTON.getType()));
        // 排序，保证菜单的有序性
        menuList.sort(Comparator.comparing(MenuDO::getSort));

        // 构建菜单树
        // 使用 LinkedHashMap 的原因，是为了排序 。实际也可以用 Stream API ，就是太丑了。
        Map<Long, AuthPermissionInfoRespVO.MenuVO> treeNodeMap = new LinkedHashMap<>();
        menuList.forEach(menu -> treeNodeMap.put(menu.getId(),
                BeanUtils.toBean(menu, AuthPermissionInfoRespVO.MenuVO.class)));
        // 处理父子关系
        treeNodeMap.values().stream().filter(node -> ObjUtil.notEqual(node.getParentId(), ID_ROOT)).forEach(childNode -> {
            // 获得父节点
            AuthPermissionInfoRespVO.MenuVO parentNode = treeNodeMap.get(childNode.getParentId());
            if (parentNode == null) {
                LoggerFactory.getLogger(getClass()).error("[buildRouterTree][resource({}) 找不到父资源({})]",
                        childNode.getId(), childNode.getParentId());
                return;
            }
            // 将自己添加到父节点中
            if (parentNode.getChildren() == null) {
                parentNode.setChildren(new ArrayList<>());
            }
            parentNode.getChildren().add(childNode);
        });
        // 获得到所有的根节点
        return filterList(treeNodeMap.values(), node -> ID_ROOT.equals(node.getParentId()));
    }

    default void fillMenuColors(List<AuthPermissionInfoRespVO.MenuVO> menus, Map<Long, MenuColorDO> colorMap) {
        if (CollUtil.isEmpty(menus)) {
            return;
        }
        menus.forEach(menu -> fillMenuColor(menu, colorMap, null));
    }

    default void fillMenuColor(AuthPermissionInfoRespVO.MenuVO menu, Map<Long, MenuColorDO> colorMap,
                               MenuColorDO firstLevelStyle) {
        final MenuColorDO resolvedFirstLevel;
        if (MenuStyleHelper.isFirstLevelMenu(menu.getParentId())) {
            resolvedFirstLevel = MenuStyleHelper.resolveFirstLevelStyle(menu.getStyleId(), colorMap);
        } else {
            resolvedFirstLevel = firstLevelStyle;
        }
        MenuColorDO effective = MenuStyleHelper.resolveEffectiveStyle(
                menu.getParentId(), menu.getStyleId(), resolvedFirstLevel, colorMap);
        MenuStyleHelper.applyStyle(effective, new MenuStyleHelper.StyleTarget() {
            @Override
            public void setStyleId(Long styleId) {
                menu.setStyleId(styleId);
            }

            @Override
            public void setColor(String color) {
                menu.setColor(color);
            }

            @Override
            public void setShape(String shape) {
                menu.setShape(shape);
            }
        });
        if (CollUtil.isNotEmpty(menu.getChildren())) {
            menu.getChildren().forEach(child -> fillMenuColor(child, colorMap, resolvedFirstLevel));
        }
    }

    SocialUserBindReqDTO convert(Long userId, Integer userType, AuthSocialLoginReqVO reqVO);

    SmsCodeSendReqDTO convert(AuthSmsSendReqVO reqVO);

    SmsCodeUseReqDTO convert(AuthSmsLoginReqVO reqVO, Integer scene, String usedIp);

}
