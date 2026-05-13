package com.yiliiii.project.my_photography_project.repository;

import com.yiliiii.project.my_photography_project.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

        interface MapPhotoProjection {
                Long getId();

                String getTitle();

                Double getLatitude();

                Double getLongitude();

                String getThumbnailUrl();

                String getImageUrl();

                java.time.LocalDateTime getTakenAt();
        }

        @Override
        @EntityGraph(attributePaths = { "tags", "usersWhoLiked", "uploader" })
        @NonNull
        Page<Photo> findAll(@NonNull Pageable pageable);

        @EntityGraph(attributePaths = { "tags", "usersWhoLiked", "uploader" })
        Page<Photo> findByTitleContainingIgnoreCaseOrDescriptionShortContainingIgnoreCase(String titleQuery,
                        String descQuery, Pageable pageable);

        Page<Photo> findByColorPaletteContaining(String color, Pageable pageable);

        @Query("SELECT p FROM Photo p " +
                        "LEFT JOIN FETCH p.tags " +
                        "LEFT JOIN FETCH p.usersWhoLiked " +
                        "LEFT JOIN FETCH p.comments c " +
                        "LEFT JOIN FETCH c.user " +
                        "LEFT JOIN FETCH p.uploader " +
                        "LEFT JOIN FETCH p.albums " +
                        "WHERE p.id = :id")
        Optional<Photo> findByIdWithDetails(@Param("id") Long id);

        List<Photo> findByTakenAtIsNull();

        @Query("SELECT p.cameraModel, COUNT(p) FROM Photo p WHERE p.cameraModel IS NOT NULL GROUP BY p.cameraModel ORDER BY COUNT(p) DESC")
        List<Object[]> countPhotosByCamera();

        @Query("SELECT p.focalLength, COUNT(p) FROM Photo p WHERE p.focalLength IS NOT NULL GROUP BY p.focalLength ORDER BY COUNT(p) DESC")
        List<Object[]> countPhotosByFocalLength();

        @Query("SELECT p.id FROM Photo p")
        List<Long> findAllIds();

        @Query("""
                        SELECT
                                p.id AS id,
                                p.title AS title,
                                p.latitude AS latitude,
                                p.longitude AS longitude,
                                p.thumbnailUrl AS thumbnailUrl,
                                p.imageUrl AS imageUrl,
                                p.takenAt AS takenAt
                        FROM Photo p
                        WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL
                        ORDER BY p.takenAt DESC, p.id DESC
                        """)
        List<MapPhotoProjection> findMapPhotoData();

        @Query("SELECT HOUR(p.takenAt), COUNT(p) FROM Photo p WHERE p.takenAt IS NOT NULL GROUP BY HOUR(p.takenAt) ORDER BY HOUR(p.takenAt)")
        List<Object[]> countPhotosByHour();

        @Query("SELECT SUBSTRING_INDEX(p.colorPalette, ',', 1) FROM Photo p WHERE p.colorPalette IS NOT NULL AND p.colorPalette <> ''")
        List<String> findAllColorPalettes();
}
