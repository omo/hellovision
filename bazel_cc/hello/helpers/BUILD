package(default_visibility = ["//visibility:public"])

cc_library(
    name = "helpers",
    srcs = [
      "equalize.cc",
      "image.cc",
      "io.cc",
      "trace.cc",
    ],
    hdrs = [
      "equalize.h",
      "image.h",
      "io.h",
      "histogram.h",
      "trace.h",
    ],
    deps = [
        "@png_archive//:png",
        "@com_github_gflags_gflags//:gflags",
        "@com_google_absl//absl/time",
    ]
)
