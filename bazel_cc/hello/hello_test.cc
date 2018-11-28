#include "gtest/gtest.h"

class HelloTest : public ::testing::Test { };

TEST_F(HelloTest, Hello) {
    ASSERT_TRUE(true);
}