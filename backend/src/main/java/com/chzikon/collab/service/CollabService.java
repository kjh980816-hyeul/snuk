package com.chzikon.collab.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.collab.domain.ClientLogo;
import com.chzikon.collab.domain.CollabGame;
import com.chzikon.collab.domain.ContentVideo;
import com.chzikon.collab.dto.CollabDtos.*;
import com.chzikon.collab.repository.ClientLogoRepository;
import com.chzikon.collab.repository.CollabGameRepository;
import com.chzikon.collab.repository.ContentVideoRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.util.ExternalUrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollabService {

    private final CollabGameRepository gameRepository;
    private final ContentVideoRepository videoRepository;
    private final ClientLogoRepository logoRepository;
    private final ExternalUrlValidator urlValidator;
    private final AdminLogService adminLogService;

    // ===== 공개 조회 =====
    @Transactional(readOnly = true)
    public List<CollabGame> games() {
        return gameRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<ContentVideo> videos() {
        return videoRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<ClientLogo> logos() {
        return logoRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    // ===== CollabGame CRUD =====
    @Transactional
    public CollabGame createGame(CollabGameRequest req, Long actorId) {
        urlValidator.validateNullable(req.thumbnailUrl());
        urlValidator.validateNullable(req.gameLinkUrl());
        urlValidator.validateNullable(req.reviewLinkUrl());
        CollabGame saved = gameRepository.save(new CollabGame(req.name(), req.description(),
                req.thumbnailUrl(), req.gameLinkUrl(), req.reviewLinkUrl(), req.campaignId(), req.sortOrder()));
        adminLogService.record(actorId, "COLLAB_GAME_CREATE", "collab_game", saved.getId(), req.name());
        return saved;
    }

    @Transactional
    public CollabGame updateGame(Long id, CollabGameRequest req, Long actorId) {
        urlValidator.validateNullable(req.thumbnailUrl());
        urlValidator.validateNullable(req.gameLinkUrl());
        urlValidator.validateNullable(req.reviewLinkUrl());
        CollabGame game = gameRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        game.update(req.name(), req.description(), req.thumbnailUrl(),
                req.gameLinkUrl(), req.reviewLinkUrl(), req.campaignId(), req.sortOrder());
        adminLogService.record(actorId, "COLLAB_GAME_UPDATE", "collab_game", id, req.name());
        return game;
    }

    @Transactional
    public void deleteGame(Long id, Long actorId) {
        gameRepository.deleteById(id);
        adminLogService.record(actorId, "COLLAB_GAME_DELETE", "collab_game", id, null);
    }

    // ===== ContentVideo CRUD =====
    @Transactional
    public ContentVideo createVideo(ContentVideoRequest req, Long actorId) {
        urlValidator.validate(req.videoUrl());
        urlValidator.validateNullable(req.thumbnailUrl());
        ContentVideo saved = videoRepository.save(new ContentVideo(req.title(), req.videoUrl(),
                req.thumbnailUrl(), req.featured(), req.sortOrder()));
        adminLogService.record(actorId, "VIDEO_CREATE", "content_video", saved.getId(), req.title());
        return saved;
    }

    @Transactional
    public ContentVideo updateVideo(Long id, ContentVideoRequest req, Long actorId) {
        urlValidator.validateNullable(req.videoUrl());
        urlValidator.validateNullable(req.thumbnailUrl());
        ContentVideo video = videoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        video.update(req.title(), req.videoUrl(), req.thumbnailUrl(), req.featured(), req.sortOrder());
        adminLogService.record(actorId, "VIDEO_UPDATE", "content_video", id, req.title());
        return video;
    }

    @Transactional
    public void deleteVideo(Long id, Long actorId) {
        videoRepository.deleteById(id);
        adminLogService.record(actorId, "VIDEO_DELETE", "content_video", id, null);
    }

    // ===== ClientLogo CRUD =====
    @Transactional
    public ClientLogo createLogo(ClientLogoRequest req, Long actorId) {
        urlValidator.validate(req.logoUrl());
        urlValidator.validateNullable(req.linkUrl());
        ClientLogo saved = logoRepository.save(new ClientLogo(req.name(), req.logoUrl(),
                req.linkUrl(), req.sortOrder()));
        adminLogService.record(actorId, "LOGO_CREATE", "client_logo", saved.getId(), req.name());
        return saved;
    }

    @Transactional
    public ClientLogo updateLogo(Long id, ClientLogoRequest req, Long actorId) {
        urlValidator.validateNullable(req.logoUrl());
        urlValidator.validateNullable(req.linkUrl());
        ClientLogo logo = logoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        logo.update(req.name(), req.logoUrl(), req.linkUrl(), req.sortOrder());
        adminLogService.record(actorId, "LOGO_UPDATE", "client_logo", id, req.name());
        return logo;
    }

    @Transactional
    public void deleteLogo(Long id, Long actorId) {
        logoRepository.deleteById(id);
        adminLogService.record(actorId, "LOGO_DELETE", "client_logo", id, null);
    }
}
