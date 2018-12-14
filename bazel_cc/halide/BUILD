load("@halide//:halide.bzl", "halide_library")

cc_binary(
  name = "halide",
  srcs = ["main.cc"],
  deps = [
    ":hello",
    ":lesson02",
    "//hello/helpers",
    "@com_github_gflags_gflags//:gflags",
    "@halide//:halide_buffer",
  ]
)

halide_library(
  name = "hello",
  srcs = ["hello.cc"],
  extra_outputs=["stmt", "schedule"],
)

halide_library(
  name = "lesson02",
  srcs = ["lesson02.cc"],
  extra_outputs=["stmt", "schedule"],
)