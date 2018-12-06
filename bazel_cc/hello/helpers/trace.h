#ifndef HELLO_HELPERS_TRACE_H
#define HELLO_HELPERS_TRACE_H

namespace hv {
  class Trace {
  public:
    Trace(const Trace& other) = delete;

    Trace(const char* label) { begin(label); }
    ~Trace() { end(); }

    static void begin(const char* label);
    static void end();
  };
}

#endif // HELLO_HELPERS_TRACE_H