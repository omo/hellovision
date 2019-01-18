
#include "Halide.h"

// http://halide-lang.org/tutorials/tutorial_lesson_07_multi_stage_pipelines.html

namespace {

namespace h = Halide;

class Lesson07 : public h::Generator<Lesson07> {
 public:
  GeneratorParam<bool> vectorize{"vectorize", true};
  GeneratorParam<bool> parallelize{"parallelize", true};

  Input<Buffer<uint8_t>> input{"input", 3};
  Output<Buffer<uint8_t>> output{"output", 3};

  void generate() {
    h::Func input_16("input_16");
    input_16(x, y, c) = h::cast<uint16_t>(input(x, y, c));

    h::Func blur_x("blur_x");
    blur_x(x, y, c) = (input_16(x -1, y, c) + input_16(x, y, c)*2 + input_16(x + 1, y, c)) / 4;

    h::Func blur_y("blur_y");
    blur_y(x, y, c) = (blur_x(x, y - 1, c) + blur_x(x, y, c)*2 + blur_x(x, y + 1, c)) / 4;

    output(x, y, c) = h::cast<uint8_t>(blur_y(x, y, c));
  }

  void schedule() {
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

HALIDE_REGISTER_GENERATOR(Lesson07, lesson07);
