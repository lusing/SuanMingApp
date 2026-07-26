package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.Lesson
import cn.xulun.bazi.model.LiuRenPlate
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 四课盘：四列（一/二/三/四课），每列上神(大,五行色) / 下神(干或支)。 */
@Composable
fun LiurenFourLessonGrid(plate: LiuRenPlate, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        plate.lessons.forEach { lesson ->
            LessonColumn(lesson, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LessonColumn(lesson: Lesson, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${lesson.index}课", fontSize = 11.sp, color = SeedRed, fontWeight = FontWeight.Bold)
            Text(
                lesson.top.chinese,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = elementColor(lesson.top.element),
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                lesson.bottomText,
                fontSize = 14.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
