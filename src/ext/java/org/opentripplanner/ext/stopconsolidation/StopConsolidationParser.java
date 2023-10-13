package org.opentripplanner.ext.stopconsolidation;

import com.csvreader.CsvReader;
import com.google.common.collect.ImmutableListMultimap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopConsolidationParser {

  private static final Logger LOG = LoggerFactory.getLogger(StopConsolidationParser.class);

  private record StopGroupEntry(String groupId, FeedScopedId stopId, boolean isPrimary) {}

  public static List<ConsolidatedStopGroup> parseGroups(File file) {
    try {
      LOG.debug("Reading stop consolidation data from {}", file.getAbsolutePath());

      var stream = new FileInputStream(file);
      var reader = new CsvReader(stream, StandardCharsets.UTF_8);
      reader.setDelimiter(',');

      reader.readHeaders();

      var entries = new ArrayList<StopGroupEntry>();
      while (reader.readRecord()) {
        var id = reader.get("stop_group_id");
        var feedId = reader.get("feed_id");
        var stopId = reader.get("stop_id");
        var isPrimary = BooleanUtils.toBoolean(Integer.parseInt(reader.get("is_primary")));
        var entry = new StopGroupEntry(id, new FeedScopedId(feedId, stopId), isPrimary);
        entries.add(entry);
      }

      var groups = entries
        .stream()
        .collect(
          ImmutableListMultimap.<StopGroupEntry, String, StopGroupEntry>flatteningToImmutableListMultimap(
            x -> x.groupId,
            Stream::of
          )
        );

      return groups
        .keys()
        .stream()
        .map(key -> {
          var ge = groups.get(key);

          var primaryId = ge.stream().filter(e -> e.isPrimary).findAny().orElseThrow().stopId;
          var secondaries = ge
            .stream()
            .filter(e -> !e.isPrimary)
            .map(e -> e.stopId)
            .collect(Collectors.toSet());

          return new ConsolidatedStopGroup(primaryId, secondaries);
        })
        .distinct()
        .toList();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
