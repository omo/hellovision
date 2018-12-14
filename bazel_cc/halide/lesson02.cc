
#include "Halide.h"

// http://halide-lang.org/tutorials/tutorial_lesson_02_input_image.html

namespace {

namespace h = Halide;

class Lesson02 : public h::Generator<Lesson02> {
 public:
  GeneratorParam<bool> vectorize{"vectorize", true};
  GeneratorParam<bool> parallelize{"parallelize", true};

  Input<Buffer<uint8_t>> input{"input", 3};
  Output<Buffer<uint8_t>> output{"output", 3};

  void generate() {
    h::Expr inval = h::cast<float>(input(x, y, c));
    h::Expr outval = h::min(255.0f, inval * 1.5f);
    output(x, y, c) = h::cast<uint8_t>(outval);
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

HALIDE_REGISTER_GENERATOR(Lesson02, lesson02);
