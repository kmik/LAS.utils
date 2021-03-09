package LASio;

public class LasVariableLengthRecord {

  final long offset;
  final String userId;
  final int recordId;
  final int recordLength; // not including header
  final String description;

  LasVariableLengthRecord(
    long offset,
    String userID,
    int recordID,
    int recordLength,
    String description) {
    this.offset = offset;
    this.userId = userID;
    this.recordId = recordID;
    this.recordLength = recordLength;
    this.description = description;
  }

  /**
   * Gets the file position for the start of the data associated with
   * this record. The data is stored as a series of bytes of the
   * length given by the record-length element.
   *
   * @return a positive long integer.
   */
  public long getFilePosition() {
    return offset;
  }

  /**
   * Gets the user ID for the record.
   *
   * @return a valid, potentially empty string.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Gets the numerical ID associated with the record.
   *
   * @return a positive value in the range of an unsigned short (two-byte)
   * integer.
   */
  public int getRecordId() {
    return recordId;
  }

  /**
   * Gets the length, in bytes, of the data associated with the record.
   *
   * @return a positive value in the range of an unsigned short (two-byte)
   * integer.
   */
  public int getRecordLength() {
    return recordLength;
  }

  /**
   * Get the description text associated with the record.
   *
   * @return a valid, potentially empty, string.
   */
  public String getDescription() {
    return description;
  }

  @Override
  public String toString(){
    return String.format(
      "Variable Length Record: %6d  %6d    %-16s  %s",
      recordId, recordLength, userId, description);
  }

}
