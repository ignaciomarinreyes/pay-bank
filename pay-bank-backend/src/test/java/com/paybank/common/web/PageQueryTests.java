package com.paybank.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageQueryTests {

    @Test
    void appliesDefaultsAndBuildsAnUnsortedPageable() {
        PageQuery query = PageQuery.of(null, null, null);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.toPageable().getPageNumber()).isZero();
        assertThat(query.toPageable().getPageSize()).isEqualTo(20);
        assertThat(query.toPageable().getSort().isSorted()).isFalse();
    }

    @Test
    void keepsExplicitValues() {
        PageQuery query = PageQuery.of(2, 100, "x");

        assertThat(query.toPageable().getPageNumber()).isEqualTo(2);
        assertThat(query.toPageable().getPageSize()).isEqualTo(100);
        assertThat(query.q()).isEqualTo("x");
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> PageQuery.of(-1, 20, null)).isInstanceOf(InvalidPageQueryException.class);
        assertThatThrownBy(() -> PageQuery.of(0, 0, null)).isInstanceOf(InvalidPageQueryException.class);
        assertThatThrownBy(() -> PageQuery.of(0, 101, null)).isInstanceOf(InvalidPageQueryException.class);
    }
}
