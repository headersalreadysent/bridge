import com.afollestad.ason.AsonName;
import com.afollestad.bridge.annotations.ContentType;

/** @author Aidan Follestad (afollestad) */
@ContentType(value = "application/json")
class RequestConvertTestObj {

  String name;
  int born;

  @AsonName(name = "data.id")
  int id;

  @AsonName(name = "data.sort")
  int sort;

  RequestConvertTestObj(String name, int born, int id, int sort) {
    this.name = name;
    this.born = born;
    this.id = id;
    this.sort = sort;
  }
}
