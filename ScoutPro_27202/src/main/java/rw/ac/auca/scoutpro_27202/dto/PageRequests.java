package rw.ac.auca.scoutpro_27202.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// Keeps page numbers sane: negative pages become 0, and a client cannot ask for 10,000 rows.
public final class PageRequests {

    private PageRequests() {}

    public static Pageable of(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(100, Math.max(size, 1));
        return PageRequest.of(safePage, safeSize);
    }
}
