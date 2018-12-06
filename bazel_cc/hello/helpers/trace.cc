#include "hello/helpers/trace.h"

#include <iostream>
#include "gflags/gflags.h"
#include "absl/base/call_once.h"
#include "absl/time/clock.h"

DEFINE_bool(enable_tracing, false, "Enable Tracing output");

namespace hv {

namespace {

struct Span {
  absl::Time begin;
  absl::Time end;
  std::string label;

  absl::Duration duration() const { return end - begin; }
};

class TraceContext {
public:
  static TraceContext* instance();

  void push(const std::string&& label) {
    spans_.push_back(Span{absl::Now(),  absl::InfinitePast(), label});
  }

  Span pop() {
    // TODO(morrita): Add some guard against invalid pop operations.
    // https://stackoverflow.com/questions/43416087/c-stl-container-pop-with-move
    Span span = std::move(spans_.back());
    spans_.pop_back();
    span.end = absl::Now();
    return span;
  }

private:
  std::vector<Span> spans_;

  static void initialize();
  static absl::once_flag initialize_once_;
  static TraceContext* instance_;
};

absl::once_flag TraceContext::initialize_once_;
TraceContext* TraceContext::instance_;

TraceContext* TraceContext::instance() {
  absl::call_once(initialize_once_, &TraceContext::initialize);
  return instance_;
}

void TraceContext::initialize() {
  instance_ = new TraceContext();
}

}

void Trace::begin(const char* label) {
  if (!FLAGS_enable_tracing) {
    return;
  }

  TraceContext::instance()->push(label);
}

void Trace::end() {
  if (!FLAGS_enable_tracing) {
    return;
  }

  Span span = TraceContext::instance()->pop();
  absl::Duration dur = span.duration();
  std::cerr << "[" << span.label << "] " << absl::ToInt64Milliseconds(dur) << "ms" << std::endl;
}

}