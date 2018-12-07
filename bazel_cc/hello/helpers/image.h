#ifndef HELPERS_IMAGE_H
#define HELPERS_IMAGE_H

#include <cstddef>
#include <cstdint>
#include <vector>
#include <algorithm>

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

    const T* row(int y) const { return buffer_.data() + (y * width_ * Pixels); }
    T* row(int y) { return buffer_.data() + (y * width_ * Pixels); }
    size_t row_size() const { return width_ * Pixels; }

    void set(size_t x, size_t y, size_t c, T value) {
        // TODO(morrita): Some assert here.
        buffer_[index(x, y, c)] = value;
    }

    T get(size_t x, size_t y, size_t c) const {
        // TODO(morrita): Some assert here.
        return buffer_[index(x, y, c)];
    }

     Image vflip() const {
        Image result(width(), height());

        for (auto y = 0u; y < height(); ++y) {
            std::copy(row(y), row(y) + row_size(), result.row(height() - y - 1));
        }

        return result;
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
RgbImage to_rgb_as_is(const hv::RawImage& raw);


} // namespace hv

#endif // HELPERS_IMAGE_H