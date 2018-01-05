package uk.co.goblinoid

import java.time.LocalDate

case class Book(title: String,
                description: String,
                authors: Seq[String],
                published: String,
                isbn: Long,
                imgUrl: String,
               )
