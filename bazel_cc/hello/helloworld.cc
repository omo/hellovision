
#include <iostream>

#include "hello/helpers/image.h"
#include "hello/helpers/io.h"

// There are here just to confirm they are buildable.
#include "json/json.h"
#include "absl/strings/string_view.h"
#include "gflags/gflags.h"

DEFINE_string(out, "/tmp/hello.png", "Full path filename to output png.");
DEFINE_string(in, "/tmp/hello.raw16", "Full path filename to input raw16 binary.");

namespace hv {

RawImage to_raw(const BayerImage& src) {
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

}

int main(int argc, char** argv) {
    gflags::ParseCommandLineFlags(&argc, &argv, true);

    hv::BayerImage bayer = hv::read_phone_raw16(FLAGS_in);
    hv::RawImage raw = hv::to_raw(bayer);
    hv::write_png(FLAGS_out, hv::to_rgb_as_is(raw.vflip()));
    std::cout << "Hello world!" << std::endl;
    return 0;
}
