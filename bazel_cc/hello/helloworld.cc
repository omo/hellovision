
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


}

int main(int argc, char** argv) {
    gflags::ParseCommandLineFlags(&argc, &argv, true);

    hv::BayerImage bayer = hv::read_phone_raw16(FLAGS_in);
    hv::RawImage raw = hv::to_raw(bayer);
    hv::write_png(FLAGS_out, hv::to_rgb_as_is(raw.vflip()));
    std::cout << "Hello world!" << std::endl;
    return 0;
}
