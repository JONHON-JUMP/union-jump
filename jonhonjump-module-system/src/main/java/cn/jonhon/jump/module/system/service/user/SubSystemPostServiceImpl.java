package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPostPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPostRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPostSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemPostDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemPostMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUserPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SubSystemPostServiceImpl implements SubSystemPostService {

    @Resource
    private SubSystemPostMapper subSystemPostMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemUserPostMapper subSystemUserPostMapper;

    @Override
    public PageResult<SubSystemPostRespVO> getSubSystemPostPage(SubSystemPostPageReqVO pageReqVO) {
        if (pageReqVO.getSubSystemId() != null) {
            validateSubSystemExists(pageReqVO.getSubSystemId());
        }
        PageResult<SubSystemPostDO> pageResult = subSystemPostMapper.selectPage(pageReqVO);
        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public SubSystemPostRespVO getSubSystemPost(Long id) {
        SubSystemPostDO post = validateSubSystemPostExists(id);
        return buildResp(post);
    }

    @Override
    public Long createSubSystemPost(SubSystemPostSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        validatePostDuplicate(createReqVO.getSubSystemId(), createReqVO.getName(), createReqVO.getCode(), null);

        SubSystemPostDO post = BeanUtils.toBean(createReqVO, SubSystemPostDO.class);
        subSystemPostMapper.insert(post);
        return post.getId();
    }

    @Override
    public void updateSubSystemPost(SubSystemPostSaveReqVO updateReqVO) {
        SubSystemPostDO post = validateSubSystemPostExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validatePostDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getName(), updateReqVO.getCode(), updateReqVO.getId());

        SubSystemPostDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemPostDO.class);
        updateObj.setSubSystemId(post.getSubSystemId());
        subSystemPostMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemPost(Long id) {
        validateSubSystemPostExists(id);
        validatePostNotAssigned(id);
        subSystemPostMapper.deleteById(id);
        subSystemUserPostMapper.deleteListByPostId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemPostList(List<Long> ids) {
        ids.forEach(id -> {
            validateSubSystemPostExists(id);
            validatePostNotAssigned(id);
        });
        subSystemPostMapper.deleteByIds(ids);
        subSystemUserPostMapper.deleteListByPostIds(ids);
    }

    private List<SubSystemPostRespVO> buildRespList(List<SubSystemPostDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(list, SubSystemPostDO::getSubSystemId)),
                SubSystemDO::getId);
        return list.stream().map(post -> {
            SubSystemPostRespVO vo = BeanUtils.toBean(post, SubSystemPostRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(post.getSubSystemId());
            if (subSystem != null) {
                vo.setClientName(subSystem.getSystemName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private SubSystemPostRespVO buildResp(SubSystemPostDO post) {
        List<SubSystemPostRespVO> list = buildRespList(Collections.singletonList(post));
        return list.isEmpty() ? BeanUtils.toBean(post, SubSystemPostRespVO.class) : list.get(0);
    }

    private SubSystemDO validateSubSystemExists(Long subSystemId) {
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        return subSystem;
    }

    private SubSystemPostDO validateSubSystemPostExists(Long id) {
        SubSystemPostDO post = subSystemPostMapper.selectById(id);
        if (post == null) {
            throw exception(SUB_SYSTEM_POST_NOT_EXISTS);
        }
        return post;
    }

    private void validatePostNotAssigned(Long postId) {
        Long count = subSystemUserPostMapper.selectCountByPostId(postId);
        if (count != null && count > 0) {
            throw exception(SUB_SYSTEM_POST_HAS_USERS);
        }
    }

    private void validatePostDuplicate(Long subSystemId, String name, String code, Long id) {
        SubSystemPostDO post = subSystemPostMapper.selectBySubSystemIdAndName(subSystemId, name);
        if (post != null && !ObjectUtil.equal(post.getId(), id)) {
            throw exception(SUB_SYSTEM_POST_NAME_DUPLICATE, name);
        }
        post = subSystemPostMapper.selectBySubSystemIdAndCode(subSystemId, code);
        if (post != null && !ObjectUtil.equal(post.getId(), id)) {
            throw exception(SUB_SYSTEM_POST_CODE_DUPLICATE, code);
        }
    }

}
