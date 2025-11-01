package ai.content.auto.controller;

import ai.content.auto.dtos.*;
import ai.content.auto.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Slf4j
public class ContentController {

        private final ContentService contentService;

        @PostMapping("/generate")
        public ResponseEntity<BaseResponse<ContentGenerateResponse>> generateContent(
                        @Valid @RequestBody ContentGenerateRequest request) {

                log.info("Generating content for content type: {}", request.getContentType());

                ContentGenerateResponse response = contentService.generateContent(request);

                BaseResponse<ContentGenerateResponse> baseResponse = new BaseResponse<ContentGenerateResponse>()
                                .setErrorMessage("Content generated successfully")
                                .setData(response);

                return ResponseEntity.ok(baseResponse);
        }

        @PostMapping
        public ResponseEntity<BaseResponse<ContentGenerationDto>> saveContent(
                        @Valid @RequestBody ContentSaveRequest request) {

                log.info("Saving content with title: {}", request.getTitle());

                ContentGenerationDto response = contentService.saveContent(request);

                BaseResponse<ContentGenerationDto> baseResponse = new BaseResponse<ContentGenerationDto>()
                                .setErrorMessage("Content saved successfully")
                                .setData(response);

                return ResponseEntity.status(HttpStatus.CREATED).body(baseResponse);
        }

        @PostMapping("/workflow")
        public ResponseEntity<BaseResponse<Map<String, Object>>> triggerWorkflow(
                        @Valid @RequestBody ContentWorkflowRequest request) {

                log.info("Triggering workflow for content type: {}", request.getContentType());

                Map<String, Object> response = contentService.triggerWorkflow(request);

                BaseResponse<Map<String, Object>> baseResponse = new BaseResponse<Map<String, Object>>()
                                .setErrorMessage("Workflow triggered successfully")
                                .setData(response);

                return ResponseEntity.ok(baseResponse);
        }

        @GetMapping
        public ResponseEntity<BaseResponse<List<ContentGenerationDto>>> getUserContents() {

                log.info("Retrieving user contents");

                List<ContentGenerationDto> contents = contentService.getUserContents();

                BaseResponse<List<ContentGenerationDto>> baseResponse = new BaseResponse<List<ContentGenerationDto>>()
                                .setErrorMessage("Contents retrieved successfully")
                                .setData(contents);

                return ResponseEntity.ok(baseResponse);
        }

        @GetMapping("/{id}")
        public ResponseEntity<BaseResponse<ContentGenerationDto>> getContentById(@PathVariable Long id) {

                log.info("Retrieving content with ID: {}", id);

                ContentGenerationDto content = contentService.getContentById(id);

                BaseResponse<ContentGenerationDto> baseResponse = new BaseResponse<ContentGenerationDto>()
                                .setErrorMessage("Content retrieved successfully")
                                .setData(content);

                return ResponseEntity.ok(baseResponse);
        }
}