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

RgbImage to_rgb_as_is(const hv::RawImage& raw) {
    hv::Trace t("to_rgb_as_is");
    RgbImage rgb{raw.width(), raw.height()};
    for (size_t y = 0; y < raw.height(); ++y) {
        for (size_t x = 0; x < raw.width(); ++x) {
            // TODO(morrita): Clamp. It could be bigger than 10bit range.
            rgb.set(x, y, 0, raw.get(x, y, 0) >> 2);
            rgb.set(x, y, 1, raw.get(x, y, 1) >> 2);
            rgb.set(x, y, 2, raw.get(x, y, 2) >> 2);
        }
    }

    return rgb;
}

RgbImage to_rgb_as_is(const hv::PlaneFloat& pf) {
    hv::Trace t("to_rgb_as_is");
    RgbImage rgb{pf.width(), pf.height()};

    for (size_t y = 0; y < rgb.height(); ++y) {
        for (size_t x = 0; x < rgb.width(); ++x) {
            uint8_t lumi = static_cast<uint8_t>(pf.get(x, y, 0) * 255.0f);
            rgb.set(x, y, 0, lumi);
            rgb.set(x, y, 1, lumi);
            rgb.set(x, y, 2, lumi);
        }
    }

    return rgb;
}


RawImage to_raw(const BayerImage& src) {
    hv::Trace t("to_raw");
    RawImage dst(src.width(), src.height());

    for (auto y = 1u; y < dst.height() - 1; ++y) {
        for (auto x = 1u; x < dst.width() - 1; ++x) {
            auto dx = x % 2;
            auto dy = y % 2;
            bool isr = dx == 1 && dy == 1;
            bool isb = dx == 0 && dy == 0;
            if (isr || isb) {
                auto here = src.get(x, y, 0);
                auto diag = (src.get(x + 0, y + 1, 0) + 
                             src.get(x + 0, y - 1, 0) + 
                             src.get(x + 1, y + 0, 0) + 
                             src.get(x - 1, y + 0, 0)) / 4;
                auto adjt = (src.get(x + 1, y - 1, 0) + 
                             src.get(x + 1, y + 1, 0) + 
                             src.get(x - 1, y - 1, 0) + 
                             src.get(x - 1, y + 1, 0)) / 4;
                if (isr) {
                    // Red pixel.
                    auto r = here;
                    auto g = diag;
                    auto b = adjt;
                    dst.set(x, y, 0, r);
                    dst.set(x, y, 1, g);
                    dst.set(x, y, 2, b);
                } else  {
                    // Blue Pixel.
                    auto r = adjt;
                    auto g = diag;
                    auto b = here;
                    dst.set(x, y, 0, r);
                    dst.set(x, y, 1, g);
                    dst.set(x, y, 2, b);
                }
            } else { // Is green
                auto here = src.get(x, y, 0);
                auto havg = (src.get(x - 1, y, 0) + src.get(x + 1, y, 0)) / 2;
                auto vavg = (src.get(x, y - 1, 0) + src.get(x, y + 1, 0)) / 2;
                bool isvg = dx == 1 && dy == 0;
                if (isvg) {
                    auto r = vavg;
                    auto g = here;
                    auto b = havg;
                    dst.set(x, y, 0, r);
                    dst.set(x, y, 1, g);
                    dst.set(x, y, 2, b);
                } else {
                    auto r = havg;
                    auto g = here;
                    auto b = vavg;
                    dst.set(x, y, 0, r);
                    dst.set(x, y, 1, g);
                    dst.set(x, y, 2, b);
                }
            }
        }
    }

    return dst;
}

Plane8 to_8(const Plane16& p) {
    hv::Trace t("to_8");
    Plane8 result(p.width(), p.height());
    for (auto y = 0u; y < p.height(); ++y) {
        for (auto x  = 0u; x < p.width(); ++x) {
           result.set(x, y, 1, p.get(x, y, 1) >> 2);
        }
    }

    return result;
}

}