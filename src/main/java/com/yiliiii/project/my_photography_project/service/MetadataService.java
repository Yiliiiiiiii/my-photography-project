package com.yiliiii.project.my_photography_project.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.yiliiii.project.my_photography_project.entity.Photo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class MetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);
    private static final DateTimeFormatter EXIF_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    /**
     * 从输入流中读取 EXIF 信息并填充到 Photo 对象中
     */
    public void extractExifData(Photo photo, InputStream inputStream) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Dir != null) {
                photo.setCameraModel(ifd0Dir.getString(ExifIFD0Directory.TAG_MODEL));
                photo.setCameraMake(ifd0Dir.getString(ExifIFD0Directory.TAG_MAKE));
            }

            ExifSubIFDDirectory subIFDDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIFDDir != null) {
                photo.setAperture(subIFDDir.getDescription(ExifSubIFDDirectory.TAG_FNUMBER));
                String iso = subIFDDir.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
                if (iso != null)
                    photo.setIso("ISO " + iso);

                Rational shutter = subIFDDir.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
                if (shutter != null) {
                    String shutterString = shutter.toSimpleString(true);
                    if (!shutter.isInteger() && shutter.getDenominator() != 1) {
                        if (shutter.floatValue() >= 1)
                            shutterString = Float.toString(shutter.floatValue());
                    }
                    photo.setShutterSpeed(shutterString + " s");
                }

                Float focalLengthVal = subIFDDir.getFloat(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
                if (focalLengthVal != null) {
                    photo.setFocalLength(Math.round(focalLengthVal) + " mm");
                }

                String takenAtString = subIFDDir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (takenAtString == null)
                    takenAtString = subIFDDir.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
                if (takenAtString != null) {
                    try {
                        LocalDateTime takenAt = LocalDateTime.parse(takenAtString, EXIF_FORMATTER);
                        photo.setTakenAt(takenAt);
                    } catch (DateTimeParseException e) {
                        logger.warn("无法解析 EXIF 日期格式: {}", takenAtString);
                    }
                }
            }

            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null) {
                GeoLocation geoLocation = gpsDir.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    photo.setLatitude(geoLocation.getLatitude());
                    photo.setLongitude(geoLocation.getLongitude());
                }
            }

        } catch (Exception e) {
            logger.warn("无法解析 EXIF 数据: {}", e.getMessage());
        }
    }
}
