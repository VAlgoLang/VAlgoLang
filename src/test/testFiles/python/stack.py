from manimlib.imports import *
class Main(Scene):
    code_start = 0
    code_end = 10
    def construct(self):
        # Building code visualisation pane
        code_lines = [["let y = new Stack;"], ["y.push(2);"], ["y.push(3);"], ["y.pop();"]]
        code_block = Code_block(code_lines)
        code_text = code_block.build()
        code_text.move_to(code_frame)
        self.code_end = len(code_lines) if self.code_end > len(code_lines) else self.code_end
        code_text.scale(min(code_height / code_text.get_height(), lhs_width / code_text.get_width()))
        self.play(FadeIn(code_text[self.code_start:self.code_end]))
        # Constructing current line pointer
        pointer = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)
        # Moves the current line pointer to line 1
        self.move_arrow_to_line(1, pointer, code_block, code_text)
        # Constructing new Stack<number> "y"
        empty = Init_structure("y", 0)
        empty.all.to_edge(np.array([2.0, -1.0, 0]))
        self.play(ShowCreation(empty.all))
        # Constructs a new Rectangle_block with value 2
        testIdent = Rectangle_block("2")
        self.place_relative_to_obj(testIdent.all, code_text, 0.25, 0.0)
        self.play(FadeIn(testIdent.all))
        self.move_arrow_to_line(2, pointer, code_block, code_text)
        self.move_relative_to_obj(testIdent.all, empty.all, 0.0, 0.25)
        self.move_arrow_to_line(3, pointer, code_block, code_text)
        # Constructs a new Rectangle_block with value 3
        testIdent1 = Rectangle_block("3")
        self.place_relative_to_obj(testIdent1.all, code_text, 0.25, 0.0)
        self.play(FadeIn(testIdent1.all))
        self.move_relative_to_obj(testIdent1.all, testIdent.all, 0.0, 0.25)
        self.move_arrow_to_line(4, pointer, code_block, code_text)
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
    def move_arrow_to_line(self, line_number, pointer, code_block, code_text):
        idx = 0
        for i in range(line_number):
            idx += len(code_block.code[i])
        if idx > self.code_end:
            self.play(FadeOut(pointer), runtime=0.1)
            # [["test, test1"], ["test2", "test3"]]
            self.scroll_down(code_text, (idx - self.code_end))
            # code_text.move_to(code_frame)
        elif idx - 1 < self.code_start:
            self.play(FadeOut(pointer), runtime=0.1)
            self.scroll_up(code_text, (self.code_start - idx+1))
        line_object = code_block.get_line_at(line_number)
        self.play(FadeIn(pointer.next_to(line_object, LEFT, MED_SMALL_BUFF)))
    def scroll_down(self, group, scrolls):
        sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
        for i in range(1, 1 + scrolls):
            group[self.code_end + i - 1].next_to(group[self.code_end - 2 + i], DOWN, aligned_edge=LEFT)
            self.play(FadeOut(group[self.code_start + i - 1]), FadeIn(group[self.code_end + i - 1]),
                      group[(self.code_start + i):(self.code_end + i)].shift, sh_val * UP, run_time=0.1)
        self.code_start = self.code_start + scrolls
        self.code_end = self.code_end + scrolls
    def scroll_up(self, group, scrolls):
        sh_val = group[self.code_start].get_corner(UP + LEFT)[1] - group[self.code_start + 1].get_corner(UP + LEFT)[1]
        for i in range(1, 1 + scrolls):
            group[self.code_start - i].next_to(group[self.code_start - i + 1], UP, aligned_edge=LEFT)
            # self.play(ReplacementTransform())
            self.play(FadeOut(group[self.code_end - i]), FadeIn(group[self.code_start - i]),
                      group[(self.code_start - i):(self.code_end - i)].shift, sh_val * DOWN, run_time=0.1)
        self.code_start = self.code_start - scrolls
        self.code_end = self.code_end - scrolls
class Code_block:
    def __init__(self, code, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for c in code:
            for sc in c:
                text = Text(sc, color=text_color, weight=text_weight, font=font)
                group.add(text)
        group.set_width(5)
        self.all = group
        self.code = code
    def build(self):
        return self.all.arrange(DOWN, aligned_edge=LEFT, center=True)
    def get_line_at(self, line_number):
        idx = 0
        for i in range(line_number):
            idx += len(self.code[i])
        return self.all[idx-1]
# Object representing a stack instantiation.
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
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)