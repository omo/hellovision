
#include "halide/mixedbag.h"
#include "halide/hello.h"
#include "halide/lesson02.h"
#include "halide/lesson07.h"
#include "halide/lesson16.h"
#include "HalideBuffer.h"
#include "gflags/gflags.h"
#include "hello/helpers/io.h"
#include <iostream>

DEFINE_string(in, "/tmp/hello.png", "The full path filename to input png.");
DEFINE_string(out, "/tmp/hello.png", "The full path filename to output png.");
DEFINE_string(pipeline, "hello", "The pipeline to run.");

namespace {

typedef Halide::Runtime::Buffer<float> FloatBuffer;
typedef Halide::Runtime::Buffer<uint8_t> ByteBuffer;

hv::PlaneFloat to_plane_float(const FloatBuffer& buffer) {
  hv::PlaneFloat result(buffer.dim(0).extent(), buffer.dim(1).extent());

  for (auto y = 0u; y < result.height(); ++y) {
    for (auto x = 0u; x < result.width(); ++x) {
      result.set(x, y, 0, buffer(x, y));
    }
  }

  return result;
}


hv::RgbImage to_rgb(const ByteBuffer& buffer) {
  hv::RgbImage result(buffer.dim(0).extent(), buffer.dim(1).extent());

  for (auto y = 0u; y < result.height(); ++y) {
    for (auto x = 0u; x < result.width(); ++x) {
      for (auto c = 0u; c < 3; ++c) {
        result.set(x, y, c, buffer(x, y, c));
      }
    }
  }

  return result;
}


ByteBuffer from_rgb(const hv::RgbImage& src) {
  ByteBuffer result(src.width(), src.height(), 3);
  for (auto y = 0; y < result.height(); ++y) {
    for (auto x = 0; x < result.width(); ++x) {
      for (auto c = 0; c < 3; ++c) {
        result(x, y, c) = src.get(x, y, c);
      }
    }
  }

  return result;
}


FloatBuffer run_hello() {
  FloatBuffer output(900, 900);
  hello(1.0f/(900*2), output);
  return output;
}

} // namespace


int main(int argc, char** argv) {
  gflags::ParseCommandLineFlags(&argc, &argv, true);

  hv::RgbImage result;

  if (FLAGS_pipeline == "hello") {
    result = hv::to_rgb_as_is(to_plane_float(run_hello()));
  } else if (FLAGS_pipeline == "lesson02") {
    ByteBuffer in = from_rgb(hv::read_png(FLAGS_in));
    ByteBuffer output(in.width(), in.height(), 3);
    lesson02(in, output);
    result = to_rgb(output);
  } else if (FLAGS_pipeline == "lesson04") {
    run_lesson04();
  } else if (FLAGS_pipeline == "lesson05") {
    run_lesson05();
  } else if (FLAGS_pipeline == "lesson07") {
    ByteBuffer in = from_rgb(hv::read_png(FLAGS_in));
    ByteBuffer output(in.width() - 2, in.height() - 2, 3);
    output.set_min(1, 1);
    lesson07(in, output);
    result = to_rgb(output);    
  } else if (FLAGS_pipeline == "lesson09") {
    run_lesson09();
  } else if (FLAGS_pipeline == "lesson13") {
    run_lesson13();
  } else if (FLAGS_pipeline == "lesson16") {
    hv::RgbImage img = hv::read_png(FLAGS_in);
    ByteBuffer in = from_rgb(img);
    in.transpose({ 2, 0, 1 });

    ByteBuffer output(img.width(), img.height(), 3);
    output.transpose({ 2, 0, 1 });
    lesson16(in, 64, output);
    result = to_rgb(output);    
  } else {
    std::cerr << "Unknown pipeline:" << FLAGS_pipeline << std::endl;
    abort();
  }

  if (!result.empty()) {
    hv::write_png(FLAGS_out, result);
  }

  std::cout << "Hello Halide!" << std::endl;
  return 0;
}