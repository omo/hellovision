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
    size_t bytes() const { return buffer_.size() * sizeof(T); }

    T* data() { return buffer_.data(); }
    const T* data() const { return buffer_.data(); }

    size_t index(size_t x, size_t y, size_t c) const {
      return y * width_ * Pixels + x * Pixels + c;
    }

    const T* row(int y) const {
      return buffer_.data() + (y * width_ * Pixels);
    }

    void set(size_t x, size_t y, size_t c, T value) {
        // TODO(morrita): Some assert here.
        buffer_[index(x, y, c)] = value;
    }

    T get(size_t x, size_t y, size_t c) const {
        // TODO(morrita): Some assert here.
        return buffer_[index(x, y, c)];
    }

private:
    size_t width_;
    size_t height_;
    std::vector<T> buffer_;
};

typedef Image<uint16_t, 1> BayerImage;
typedef Image<uint16_t, 3> RawImage;
typedef Image<uint8_t, 3> RgbImage;

RgbImage to_rgb_as_is(const hv::BayerImage& raw);

} // namespace hv

#endif // HELPERS_IMAGE_H