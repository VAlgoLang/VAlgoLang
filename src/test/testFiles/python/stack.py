from manimlib.imports import *
class Main(Scene):
    def construct(self):
        code_block = Code_block(["let y = new Stack;","y.push(2);","y.push(3);","y.pop();"])
        code_text = code_block.build()
        self.place_at(code_text, -1, 0)
        self.play(FadeIn(code_text))
        pointer = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)
        self.move_arrow_to_line(1, pointer, code_block)
        empty = Init_structure("y", 0)
        empty.all.to_edge(np.array([2.0, -1.0, 0]))
        self.play(ShowCreation(empty.all))
        testIdent = Rectangle_block("2")
        self.place_relative_to_obj(testIdent.all, code_text, 0.25, 0.0)
        self.play(FadeIn(testIdent.all))
        self.move_arrow_to_line(2, pointer, code_block)
        self.move_relative_to_obj(testIdent.all, empty.all, 0.0, 0.25)
        self.move_arrow_to_line(3, pointer, code_block)
        testIdent1 = Rectangle_block("3")
        self.place_relative_to_obj(testIdent1.all, code_text, 0.25, 0.0)
        self.play(FadeIn(testIdent1.all))
        self.move_relative_to_obj(testIdent1.all, testIdent.all, 0.0, 0.25)
        self.move_arrow_to_line(4, pointer, code_block)
        self.move_relative_to_obj(testIdent1.all, testIdent.all, 0.0, 20.25)
        self.play(FadeOut(testIdent1.all))
    def place_at(self, group, x, y):
        group.to_edge(np.array([x, y, 0]))
    def move_relative_to_edge(self, group, x, y):
        self.play(ApplyMethod(group.to_edge, np.array([x, y, 0])))
    def move_relative_to_obj(self, group, target, x, y):
        self.play(ApplyMethod(group.next_to, target, np.array([x, y, 0])))
    def place_relative_to_obj(self, group, target, x, y):
        group.next_to(target, np.array([x, y, 0]))
    def move_arrow_to_line(self, line_number, pointer, code_block):
        line_object = code_block.get_line_at(line_number)
        self.play(FadeIn(pointer.next_to(line_object, LEFT, MED_SMALL_BUFF)))
class Code_block:
    def __init__(self, code, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for c in code:
            group.add(Text(c, color=text_color, weight=text_weight, font=font))
        self.all = group
    def build(self):
        return self.all.arrange(DOWN, aligned_edge=LEFT)
    def get_line_at(self, line_number):
        return self.all[line_number - 1]
class Init_structure:
    def __init__(self, text, angle, length=1.5, color=WHITE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.shape = Line(color=color)
        self.shape.set_length(length)
        self.shape.set_angle(angle)
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.text.next_to(self.shape, DOWN, SMALL_BUFF)
        self.all = VGroup(self.text, self.shape)
class Rectangle_block:
    def __init__(self, text, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.color = color
        self.shape_text = text
        self.text_color = text_color
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)