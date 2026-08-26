package com.haru.haruverse.global.exception;

/**
 * 한 번에 처리할 수 있는 개수를 넘긴 요청.
 *
 * <p><b>왜 조용히 자르지 않는가</b>
 * 예전에는 상한을 넘으면 앞에서부터 잘라 처리했다. 그러면 호출한 쪽은
 * "요청한 게 다 처리됐다"고 믿는다 — 실제로 70건을 보냈는데 40건만 반영되고
 * 응답에는 그 사실이 없어서, 나머지 30건이 조용히 사라졌다.
 * 넘치면 거절하고 몇 개까지 되는지 알려주는 편이 낫다.
 */
public class TooManyItemsException extends RuntimeException {

    public TooManyItemsException(int requested, int max) {
        super("한 번에 최대 %d개까지 처리할 수 있습니다. (요청: %d개) 나눠서 요청해주세요."
                .formatted(max, requested));
    }
}
