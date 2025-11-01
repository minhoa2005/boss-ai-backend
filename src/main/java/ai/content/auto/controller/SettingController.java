package ai.content.auto.controller;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.VUserConfigDto;
import ai.content.auto.service.ConfigService;
import ai.content.auto.service.VUserConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/setting")
@RequiredArgsConstructor
@Slf4j
public class SettingController {
    private final VUserConfigService vUserConfigService;

    @GetMapping("/tone")
    public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getTone() {
        List<VUserConfigDto> vUserConfigDtoList = vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_TONE);
        BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<>();
        baseResponse.setData(vUserConfigDtoList);
        return ResponseEntity.ok(baseResponse);
    }
    @GetMapping("/industry")
    public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getIndustry() {
        List<VUserConfigDto> vUserConfigDtoList = vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_INDUSTRY);
        BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<>();
        baseResponse.setData(vUserConfigDtoList);
        return ResponseEntity.ok(baseResponse);
    }
    @GetMapping("/language")
    public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getLanguage() {
        List<VUserConfigDto> vUserConfigDtoList = vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_LANGUAGE);
        BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<>();
        baseResponse.setData(vUserConfigDtoList);
        return ResponseEntity.ok(baseResponse);
    }
    @GetMapping("/target-audience")
    public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getTargetAudience() {
        List<VUserConfigDto> vUserConfigDtoList = vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_TARGET_AUDIENCE);
        BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<>();
        baseResponse.setData(vUserConfigDtoList);
        return ResponseEntity.ok(baseResponse);
    }
    @GetMapping("/content-type")
    public ResponseEntity<BaseResponse<List<VUserConfigDto>>> getContentType() {
        List<VUserConfigDto> vUserConfigDtoList = vUserConfigService.findAllByCategory(ContentConstants.CATEGORY_CONTENT_TYPE);
        BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<>();
        baseResponse.setData(vUserConfigDtoList);
        return ResponseEntity.ok(baseResponse);
    }
    @PostMapping
    public ResponseEntity<BaseResponse<List<VUserConfigDto>>> update(@Valid @RequestBody VUserConfigDto vUserConfigDto) {
        vUserConfigService.UpdateConfig(vUserConfigDto);
        List<VUserConfigDto> vUserConfigDtoList = vUserConfigService.findAllByCategory(vUserConfigDto.category());
        BaseResponse<List<VUserConfigDto>> baseResponse = new BaseResponse<>();
        baseResponse.setData(vUserConfigDtoList);
        return ResponseEntity.ok(baseResponse);
    }

}
