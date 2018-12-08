#include "hello/helpers/equalize.h"

#include "hello/helpers/histogram.h"
#include "hello/helpers/trace.h"

namespace hv {

namespace {
typedef std::vector<uint8_t> Lut8;

Lut8 equalized_lut(const Histogram& hist, size_t num_pixels) {
    Lut8 result(hist.size(), 0);
    size_t levels = 256;
    size_t pixels_per_level = num_pixels / levels;
    
    size_t occupancy = 0;
    uint8_t mapped_level = 0;
    for (auto i = 0u; i < result.size(); ++i) {
        occupancy += hist[i];
        while (occupancy >= pixels_per_level) {
            mapped_level++;
            occupancy = occupancy - pixels_per_level;
        }

        result[i] = std::min(mapped_level, static_cast<uint8_t>(255u));
    }

    return result;
}

Plane8 equalize(const Plane16& src) {
    hv::Trace t("equalize");
    Histogram h = histogram_of(src);
    Lut8 lut = equalized_lut(h, src.buffer().size());
    Plane8 result(src.width(), src.height());

    for (auto y = 0u; y < src.height(); ++y) {
        for (auto x = 0u; x < src.width(); ++x) {
            result.set(x, y, 1, lut[src.get(x, y, 1)]);
        }
    }

    return result;
}

} // namespace

RgbImage equalize_to_rgb(const RawImage& src) {
    std::vector<Plane16> src_planes = split(src);
    std::vector<Plane8> dst_planes(src_planes.size());
    std::transform(src_planes.begin(), src_planes.end(), dst_planes.begin(), equalize);
    return combine<uint8_t, 3>(dst_planes);
}

} // namespace hv