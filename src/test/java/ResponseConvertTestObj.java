import com.afollestad.ason.AsonName;
import com.afollestad.bridge.annotations.ContentType;

/** @author Aidan Follestad (afollestad) */
@ContentType(value = "application/json")
class ResponseConvertTestObj {

  @AsonName(name = "json.name")
  String name;

  @AsonName(name = "json.born")
  int born;

  @AsonName(name = "json.data.id")
  int id;

  @AsonName(name = "json.data.sort")
  int sort;

  public ResponseConvertTestObj() {}
}
