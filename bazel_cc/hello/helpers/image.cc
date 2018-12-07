#include "hello/helpers/image.h"
#include "hello/helpers/trace.h"

namespace hv {

RgbImage to_rgb_as_is(const hv::BayerImage& raw) {
    hv::Trace t("to_rgb_as_is");
    RgbImage rgb{raw.width(), raw.height()};

    for (size_t y = 0; y < raw.height(); ++y) {
        for (size_t x = 0; x < raw.width(); ++x) {
            uint8_t lumi = raw.get(x, y, 0) >> 2; 
            rgb.set(x, y, 0, lumi);
            rgb.set(x, y, 1, lumi);
            rgb.set(x, y, 2, lumi);
        }
    }

    return rgb;
}

}