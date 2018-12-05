
#include <iostream>

#include "hello/helpers/image.h"
#include "hello/helpers/io.h"

// There are here just to confirm they are buildable.
#include "json/json.h"
#include "absl/strings/string_view.h"

int main() {
    hv::Image<uint8_t, 3> image(256, 256);
    for (size_t y = 0; y < image.height(); ++y) {
        for (size_t x = 0; x < image.width(); ++x) {
            uint8_t luminance = y;
            image.set(x, y, 0, luminance);
            image.set(x, y, 1, luminance);
            image.set(x, y, 2, luminance);
        }
    }

    write_png("/home/omo/tmp/hello.png", image);
    std::cout << "Hello world!" << std::endl;    
}
