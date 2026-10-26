package ma.nqiya.photo;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import ma.nqiya.common.exception.ApiException;
import ma.nqiya.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PhotoRepository photos;
    private final AppProperties props;

    public PhotoService(PhotoRepository photos, AppProperties props) {
        this.photos = photos;
        this.props = props;
    }

    /** Validates an uploaded image and returns its bytes. */
    public UploadedImage read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("PHOTO_REQUIRED", "A photo is required");
        }
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw ApiException.badRequest("PHOTO_TYPE", "Only JPEG, PNG or WebP photos are accepted");
        }
        if (file.getSize() > props.uploads().maxBytes()) {
            throw ApiException.badRequest("PHOTO_TOO_LARGE", "The photo is too large");
        }
        try {
            return new UploadedImage(type, file.getBytes());
        } catch (IOException e) {
            throw ApiException.badRequest("PHOTO_UNREADABLE", "The photo could not be read");
        }
    }

    @Transactional
    public Photo save(Photo photo) {
        return photos.save(photo);
    }

    @Transactional(readOnly = true)
    public Photo get(UUID id) {
        return photos.findById(id).orElseThrow(() -> ApiException.notFound("Photo"));
    }

    public record UploadedImage(String contentType, byte[] data) {
    }
}
