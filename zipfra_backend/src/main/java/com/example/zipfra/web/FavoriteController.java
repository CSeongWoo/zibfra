package com.example.zipfra.web;

import com.example.zipfra.dto.favorite.FavoriteDto;
import com.example.zipfra.dto.favorite.FavoriteToggleResponse;
import com.example.zipfra.security.ZipfraPrincipal;
import com.example.zipfra.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{propertyId}")
    public ResponseEntity<?> toggleFavorite(@AuthenticationPrincipal ZipfraPrincipal principal,
                                            @PathVariable Long propertyId) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "TOKEN_MISSING"));
        }

        try {
            boolean isFavorite = favoriteService.toggleFavorite(principal.getId(), propertyId);
            return ResponseEntity.ok(FavoriteToggleResponse.builder().isFavorite(isFavorite).build());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CONFLICT"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getFavorites(@AuthenticationPrincipal ZipfraPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "TOKEN_MISSING"));
        }

        List<FavoriteDto> favorites = favoriteService.getFavoritesByUserId(principal.getId());
        return ResponseEntity.ok(favorites);
    }
}
