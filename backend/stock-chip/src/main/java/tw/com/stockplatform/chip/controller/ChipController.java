package tw.com.stockplatform.chip.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.com.stockplatform.chip.dto.request.ChipGetRequest;
import tw.com.stockplatform.chip.dto.response.ChipDTO;
import tw.com.stockplatform.chip.service.ChipService;
import tw.com.stockplatform.common.audit.Audited;
import tw.com.stockplatform.common.response.ApiResponse;

/**
 * M-CHIP 籌碼 REST API。
 */
@RestController
@RequestMapping("/api/v1/chip")
@RequiredArgsConstructor
public class ChipController {

    private final ChipService chipService;

    /**
     * 查詢個股三大法人最新買賣超。
     */
    @PostMapping("/get")
    @Audited(action = "CHIP_GET", target = "stock")
    public ApiResponse<ChipDTO> getChip(@Valid @RequestBody ChipGetRequest request) {
        return ApiResponse.success(chipService.getChip(request));
    }
}
