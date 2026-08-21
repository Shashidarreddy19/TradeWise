package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.entity.DownloadHistoryEntity;
import com.cbec.ai.pipeline.repository.DownloadHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DatasetDownloaderService {

    private final HttpClient httpClient;
    private final DownloadHistoryRepository downloadHistoryRepository;

    public DatasetDownloaderService(DownloadHistoryRepository downloadHistoryRepository) {
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Checks HTTP ETag and Last-Modified headers before downloading.
     * Returns true if a newer dataset version exists, false if unchanged.
     */
    public boolean isNewerVersionAvailable(String url, String country) {
        if (url == null || !url.startsWith("http")) return true;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            Optional<String> etagOpt = response.headers().firstValue("ETag");
            Optional<String> lastModOpt = response.headers().firstValue("Last-Modified");

            if (etagOpt.isPresent() || lastModOpt.isPresent()) {
                List<DownloadHistoryEntity> history = downloadHistoryRepository.findByCountry(country);
                if (!history.isEmpty()) {
                    DownloadHistoryEntity latest = history.get(history.size() - 1);
                    if (etagOpt.isPresent() && etagOpt.get().equals(latest.getEtag())) {
                        log.info("Incremental Sync - HTTP ETag matched ({}) for {}. Dataset is unchanged.", etagOpt.get(), country);
                        return false;
                    }
                    if (lastModOpt.isPresent() && lastModOpt.get().equals(latest.getLastModified())) {
                        log.info("Incremental Sync - HTTP Last-Modified matched ({}) for {}. Dataset is unchanged.", lastModOpt.get(), country);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to perform HEAD check on official URL {}; proceeding with full download", url);
        }
        return true;
    }
}
