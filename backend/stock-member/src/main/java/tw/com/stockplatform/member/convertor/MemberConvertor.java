package tw.com.stockplatform.member.convertor;

import org.mapstruct.Mapper;
import tw.com.stockplatform.domain.po.UserPO;
import tw.com.stockplatform.member.dto.response.MemberDTO;

@Mapper(componentModel = "spring")
public interface MemberConvertor {

    MemberDTO toDTO(UserPO po);
}
