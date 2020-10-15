from manimlib.imports import *
class Main(Scene):
    def construct(self):
        code_block = Code_block(["let y = new Stack;","y.push(2);","y.push(3);","y.pop();"])
        code_text = code_block.build()
        self.place_at(code_text, -1, 0)
        self.play(FadeIn(code_text))
        pointer = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)
        self.move_arrow_to_line(1, pointer, code_block)
        empty = Init_structure("y", 0).build()
        empty.to_edge(np.array([2, -1, 0]))
        self.play(ShowCreation(empty))
        testIdent = Rectangle_block("2").build()
        self.place_relative_to_obj(testIdent, code_text, 0.25, 0.0)
        self.play(FadeIn(testIdent))
        self.move_arrow_to_line(2, pointer, code_block)
        self.move_relative_to_obj(testIdent, empty, 0.0, 0.25)
        self.move_arrow_to_line(3, pointer, code_block)
        testIdent1 = Rectangle_block("3").build()
        self.place_relative_to_obj(testIdent1, code_text, 0.25, 0.0)
        self.play(FadeIn(testIdent1))
        self.move_relative_to_obj(testIdent1, testIdent, 0.0, 0.25)
        self.move_arrow_to_line(4, pointer, code_block)
        self.move_relative_to_obj(testIdent1, testIdent, 0.0, 20.25)
        self.play(FadeOut(testIdent1))
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
    def __init__(self, code):
        group = VGroup()
        for c in code:
            group.add(TextMobject(c))
        self.group = group
    def build(self):
        return self.group.arrange(DOWN, aligned_edge=LEFT)
    def get_line_at(self, line_number):
        return self.group[line_number - 1]
class Init_structure:
    def __init__(self, text, angle, length=1.5, color=WHITE):
        self.text = text
        self.angle = angle
        self.length = length
        self.color = color
    def build(self):
        line = Line(color=self.color)
        line.set_length(self.length)
        line.set_angle(self.angle)
        label = TextMobject(self.text, color=self.color)
        label.next_to(line, DOWN, SMALL_BUFF)
        group = VGroup(label, line)
        return group
class Rectangle_block:
    def __init__(self, text, height=0.75, width=1.5, color=BLUE):
        self.text   = text
        self.height = height
        self.width  = width
        self.color  = color
    def build(self):
        inside_text = TextMobject(self.text)
        rectangle   = Rectangle(height=self.height, width=self.width, color=self.color)
        group       = VGroup(inside_text, rectangle)
        return group