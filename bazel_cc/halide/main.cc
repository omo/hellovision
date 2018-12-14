
#include "halide/hello.h"
#include "HalideBuffer.h"
#include "gflags/gflags.h"
#include "hello/helpers/io.h"
#include <iostream>

DEFINE_string(out, "/tmp/hello.png", "The full path filename to output png.");
DEFINE_string(pipeline, "hello", "The pipeline to run.");

namespace {

hv::PlaneFloat to_plane_float(Halide::Runtime::Buffer<float> buffer) {
  hv::PlaneFloat result(buffer.dim(0).extent(), buffer.dim(1).extent());

  for (auto y = 0u; y < result.height(); ++y) {
    for (auto x = 0u; x < result.width(); ++x) {
      result.set(x, y, 0, buffer(x, y));
    }
  }

  return result;
}


Halide::Runtime::Buffer<float> run_hello() {
  Halide::Runtime::Buffer<float> output(900, 900);
  hello(1.0f/(900*2), output);
  return output;
}

} // namespace


int main(int argc, char** argv) {
  gflags::ParseCommandLineFlags(&argc, &argv, true);

  hv::RgbImage result;

  if (FLAGS_pipeline == "hello") {
    result = hv::to_rgb_as_is(to_plane_float(run_hello()));
  } else {
    std::cerr << "Unknown pipeline:" << FLAGS_pipeline << std::endl;
    abort();
  }

  hv::write_png(FLAGS_out, result);

  std::cout << "Hello Halide!" << std::endl;
  return 0;
}