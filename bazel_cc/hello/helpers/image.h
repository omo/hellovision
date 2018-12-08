#ifndef HELPERS_IMAGE_H
#define HELPERS_IMAGE_H

#include <cstddef>
#include <cstdint>
#include <cassert>
#include <vector>
#include <algorithm>

namespace hv {

template<typename T, size_t Pixels>
class Image {
public:
    Image(const Image&) = default;
    Image(size_t w, size_t h)
        : width_{w}, height_{h}, buffer_(w*h*Pixels, 0) {}
    Image() : Image(0, 0) {}

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

// TODO(morrita): Consider moving algotirhms to a separate file.
template<typename T, size_t Pixel>
inline std::vector<Image<T, 1>> split(const Image<T, Pixel>& src) {
    std::vector<Image<T, 1>> result;
    for (auto i = 0u; i < Pixel; ++i) {
        Image<T, 1> slice(src.width(), src.height());
        for (auto y = 0u; y < src.height(); ++y) {
            for (auto x = 0u; x < src.width(); ++x) {
                slice.set(x, y, 1, src.get(x, y, i));
            }
        }

        result.push_back(slice);
    }

    return result;
}

template<typename T, size_t Pixel>
inline Image<T, Pixel> combine(const std::vector<Image<T, 1>>& components) {
    static_assert(0 < Pixel);
    assert(components.size() == Pixel);

    Image<T, Pixel> result(components[0].width(), components[0].height());
    assert(std::find_if(components.begin(), components.end(), [&result](auto i) {
        return i.width() != result.width() && i.height() != result.height(); }) == components.end());
    for (auto y = 0u; y < result.height(); ++y) {
        for (auto x = 0u; x < result.width(); ++x) {
            for (auto i = 0u; i < Pixel; ++i) {
                result.set(x, y, i, components[i].get(x, y, 1));
            }
        }
    }

    return result;
}


typedef Image<uint16_t, 1> BayerImage;
typedef Image<uint16_t, 3> RawImage;
typedef Image<uint8_t, 3> RgbImage;

typedef Image<uint16_t, 1> Plane16;
typedef Image<uint8_t, 1> Plane8;

RgbImage to_rgb_as_is(const hv::BayerImage& raw);
RgbImage to_rgb_as_is(const hv::RawImage& raw);
RawImage to_raw(const BayerImage& src);

Plane8 to_8(const Plane16& p);

} // namespace hv

#endif // HELPERS_IMAGE_H