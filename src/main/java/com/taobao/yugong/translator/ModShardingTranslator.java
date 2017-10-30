package com.taobao.yugong.translator;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.taobao.yugong.common.db.meta.ColumnValue;
import com.taobao.yugong.common.model.record.Record;
import com.taobao.yugong.exception.YuGongException;

import lombok.Getter;
import lombok.Setter;

import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ModShardingTranslator implements DataTranslator {

  @Getter
  @Setter
  private String shardingKeyName;

  @Getter
  @Setter
  private Integer modNumber;

  @Override
  public String translatorSchema() {
    return null;
  }

  @Override
  public String translatorTable() {
    return null;
  }

  @Override
  public boolean translator(Record record) {
    return true;
  }

  @VisibleForTesting
  int calculateShardingKey(long input) {
    return (int) ((input % modNumber + modNumber) % modNumber);
  }

  @Override
  public List<Record> translator(List<Record> records) {
    if (Strings.isNullOrEmpty(shardingKeyName)) {
      throw new YuGongException("shardingKeyName is not set");
    }
    if (modNumber == null) {
      throw new YuGongException("modNumber is not set");
    }

    return records.stream().peek(record -> {
      ColumnValue column = record.getColumnByName(shardingKeyName);
      long shardingValue;
      if (column.getValue() instanceof Integer) {
        shardingValue = (int) column.getValue();
      } else if (column.getValue() instanceof Long) {
        shardingValue = (long) column.getValue();
      } else {
        throw new YuGongException(String.format("shardingKey value is not BIGINT or INTEGER %s",
            column.getColumn().getType()));
      }
      int shardingPartition = calculateShardingKey(shardingValue);
      record.setTableName(record.getTableName() + "_" + shardingPartition);
    }).collect(Collectors.toList());
  }

}
