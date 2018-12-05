#ifndef HELPERS_IMAGE_H
#define HELPERS_IMAGE_H

#include <cstddef>
#include <cstdint>
#include <vector>

namespace hv {

template<typename T, size_t Pixels>
class Image {
public:
    Image(const Image&) = default;
    Image(size_t w, size_t h)
        : width_{w}, height_{h}, buffer_(w*h*Pixels, 0)
    {}

    size_t width() const { return width_; }
    size_t height() const { return height_; }
    const std::vector<T>& buffer() const { return buffer_; }

    void set(size_t x, size_t y, size_t c, T value) {
        // TODO(morrita): Some assert here.
        buffer_[y * width_ * Pixels + x * Pixels + c] = value;
    }

    const T* row(int y) const {
        return buffer_.data() + (y * width_ * Pixels);
    }
private:
    size_t width_;
    size_t height_;
    std::vector<T> buffer_;
};

} // namespace hv

#endif // HELPERS_IMAGE_H