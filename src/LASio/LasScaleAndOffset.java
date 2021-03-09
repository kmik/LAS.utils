package LASio;

/**
 * A container class to provide the scale and offset
 * factors obtained from the LAS file header.
 */
public class LasScaleAndOffset {

  /**
   * The x coordinate scale factor
   */
  public final double xScaleFactor;
  /**
   * The y coordinate scale factor
   */
  public final double yScaleFactor;
  /**
   * The z coordinate scale factor
   */
  public final double zScaleFactor;
  /**
   * The x coordinate offset
   */
  public final double xOffset;
  /**
   * The y coordinate offset
   */
  public final double yOffset;
  /**
   * The z coordinate offset
   */
  public final double zOffset;

  LasScaleAndOffset(
    double xScaleFactor,
    double yScaleFactor,
    double zScaleFactor,
    double xOffset,
    double yOffset,
    double zOffset
  ) {
    this.xScaleFactor = xScaleFactor;
    this.yScaleFactor = yScaleFactor;
    this.zScaleFactor = zScaleFactor;
    this.xOffset = xOffset;
    this.yOffset = yOffset;
    this.zOffset = zOffset;
  }

}
