
#include "Halide.h"

// http://halide-lang.org/tutorials/tutorial_lesson_16_rgb_generate.html

namespace {

namespace h = Halide;

class Lesson16 : public h::Generator<Lesson16> {
 public:
  Input<Buffer<uint8_t>> input{"input", 3};
  Input<uint8_t> offset{"offset"};
  Output<Buffer<uint8_t>> output{"output", 3};

  h::GeneratorParam<bool> vectorize{"vectorize", true};
  h::GeneratorParam<bool> parallelize{"parallelize", true};

  enum class Layout { Planar, Interleaved, Either, Specialized };
  h::GeneratorParam<Layout> layout{
    "layout",
    Layout::Planar,
    {{ "planar", Layout::Planar },
     { "interleaved", Layout::Interleaved },
     { "Either", Layout::Either },
     { "Specialized", Layout::Specialized}}
  };

  void generate() {
    output(x, y, c) = input(x, y, c) * offset;
  }

  void schedule() {
    if (layout == Layout::Planar) {
      // The default. 
    } else if (layout == Layout::Interleaved) {
      input
        .dim(0).set_stride(3)
        .dim(2).set_stride(1)
        .dim(2).set_bounds(0, 3);
      output
        .dim(0).set_stride(3)
        .dim(2).set_stride(1)
        .dim(2).set_bounds(0, 3);
    }

    if (vectorize) {
      output.vectorize(x, natural_vector_size<float>());
    }
    if (parallelize) {
      output.parallel(y);
    }
  }

 private:
  Var x, y, c;
};

}  // namespace

HALIDE_REGISTER_GENERATOR(Lesson16, lesson16);
