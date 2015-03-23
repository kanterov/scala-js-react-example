package example

import com.kanterov.scalajs.react.React

import scala.scalajs.js
import org.scalajs.dom

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {
    React.renderComponent(
      HelloWorld(HelloWorld.Props()),
      dom.document.body
    )
  }
}
