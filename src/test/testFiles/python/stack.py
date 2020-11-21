from abc import ABC, abstractmethod
from manimlib.imports import *
import tempfile
class Main(Scene):
    code_start = 0
    code_end = 10
    def construct(self):
        # Building code visualisation pane
        code_lines = [["let y = new Stack<number>();"], ["y.push(2);"], ["y.push(3);"], ["y.pop();"]]
        code_block = Code_block(code_lines, syntax_highlighting=True, syntax_highlighting_style="inkpot", tab_spacing=2)
        code_text = code_block.build()
        code_text.set_width(4.2)
        code_text.next_to(variable_frame, DOWN, buff=0.9)
        code_text.to_edge(buff=MED_LARGE_BUFF)
        self.code_end = len(code_lines) if self.code_end > len(code_lines) else self.code_end
        self.play(FadeIn(code_text[self.code_start:self.code_end]), run_time=1.0)
        # Constructing current line pointer
        pointer = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)
        # Moves the current line pointer to line 1
        self.move_arrow_to_line(1, pointer, code_block, code_text)
        # Constructing new Stack<number> "y"
        stack = Stack([5.0, 4.0, 0], [7.0, 4.0, 0], [5.0, -4.0, 0], [7.0, -4.0, 0], DOWN)
        self.play(*stack.create_init("y"))
        self.move_arrow_to_line(2, pointer, code_block, code_text)
        # Constructs a new Rectangle_block with value 2.0
        rectangle = Rectangle_block("2.0", stack)
        [self.play(*animation, run_time=1.0) for animation in stack.push(rectangle)]
        stack.add(rectangle.all)
        self.move_arrow_to_line(3, pointer, code_block, code_text)
        # Constructs a new Rectangle_block with value 3.0
        rectangle1 = Rectangle_block("3.0", stack)
        [self.play(*animation, run_time=1.0) for animation in stack.push(rectangle1)]
        stack.add(rectangle1.all)
        self.move_arrow_to_line(4, pointer, code_block, code_text)
        [self.play(*animation, run_time=1.0) for animation in stack.pop(rectangle1, fade_out=True)]
    def place_at(self, group, x, y):
        group.to_edge(np.array([x, y, 0]))
    def move_relative_to_edge(self, group, x, y):
        self.play(ApplyMethod(group.to_edge, np.array([x, y, 0])))
    def move_relative_to_obj(self, group, target, x, y):
        self.play(ApplyMethod(group.next_to, target, np.array([x, y, 0])))
    def place_relative_to_obj(self, group, target, x, y):
        group.next_to(target, np.array([x, y, 0]))
    def fade_out_if_needed(self, mobject):
        if mobject in self.mobjects:
            return FadeOut(mobject)
        else:
            return None
    def move_arrow_to_line(self, line_number, pointer, code_block, code_text):
        idx = 0
        for i in range(line_number):
            idx += len(code_block.code[i])
        if idx > self.code_end:
            animation = self.fade_out_if_needed(pointer)
            if animation is not None:
                self.play(animation, runtime=0.1)
            self.scroll_down(code_text, (idx - self.code_end))
        elif idx - 1 < self.code_start:
            animation = self.fade_out_if_needed(pointer)
            if animation is not None:
                self.play(animation, runtime=0.1)
            self.scroll_up(code_text, (self.code_start - idx+len(code_block.code[line_number-1])))
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
    def __init__(self, code, syntax_highlighting=True, syntax_highlighting_style="inkpot", text_color=WHITE, text_weight=NORMAL, font="Times New Roman", tab_spacing=2):
        group = VGroup()
        if syntax_highlighting:
            fp = tempfile.NamedTemporaryFile(suffix='.re')
            for c in code:
                for sc in c:
                    fp.write(bytes(sc + "\n", encoding='utf-8'))
            fp.seek(0)
            self.paragraph = Code(fp.name, style=syntax_highlighting_style, language="reasonml", line_spacing=0.2,
                              tab_width=tab_spacing).code
            fp.close()
            group.add(self.paragraph)
            group.set_width(5)
            self.all = self.paragraph
        else:
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
class DataStructure(ABC):
    def __init__(self, ul, ur, ll, lr, aligned_edge, color=WHITE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.ul = ul
        self.ur = ur
        self.ll = ll
        self.lr = lr
        self.max_width = self.lr[0] - self.ul[0]
        self.width_center = self.ul[0] + self.max_width / 2
        self.max_height = self.ul[1] - self.lr[1]
        self.aligned_edge = aligned_edge
        self.color = color
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font
        self.all = VGroup()
    def shrink(self, new_width, new_height):
        scale_factor = min((self.max_width - 2 * MED_SMALL_BUFF) / new_width, (self.max_height - 2 * MED_SMALL_BUFF) / new_height)
        if scale_factor != 1:
            return ApplyMethod(self.all.scale, scale_factor, {"about_edge": self.aligned_edge}), scale_factor
        return 0, 1
    def shrink2(self, new_width, new_height):
        scale_factor = min((self.max_width - 2 * MED_SMALL_BUFF) / new_width,
                           (self.max_height - 2 * MED_SMALL_BUFF) / new_height)
        if scale_factor != 1:
            return [ScaleInPlace(self.all, scale_factor),
                    ApplyMethod(self.all.move_to, self.aligned_edge)], scale_factor
        return 0, 1
    def will_cross_boundary(self, object_dim, boundary_name):
        boundary_options = {"TOP": self.will_cross_top_boundary,
                            "RIGHT": self.will_cross_right_boundary,
                            "BOTTOM": self.will_cross_bottom_boundary,
                            "LEFT": self.will_cross_left_boundary}
        return boundary_options[boundary_name](object_dim)
    def will_cross_top_boundary(self, object_height):
        frame_top_y = self.ul[1]
        group_top_y = self.all.get_top()[1]
        return group_top_y + object_height > frame_top_y
    def will_cross_bottom_boundary(self, object_height):
        frame_bottom_y = self.ll[1]
        group_bottom_y = self.all.get_bottom()[1]
        return group_bottom_y - object_height < frame_bottom_y
    def will_cross_right_boundary(self, object_width):
        frame_right_x = self.lr[0]
        group_right_x = self.all.get_right()[0]
        return group_right_x + object_width > frame_right_x
    def will_cross_left_boundary(self, object_width):
        frame_left_x = self.ll[0]
        group_left_x = self.all.get_left()[0]
        return group_left_x - object_width < frame_left_x
    def has_crossed_top_boundary(self):
        frame_top_y = self.ul[1]
        group_top_y = self.all.get_top()[1]
        return group_top_y > frame_top_y
    def add(self, obj):
        self.all.add(obj)
    @abstractmethod
    def create_init(self, ident):
        pass
    @abstractmethod
    def shrink_if_cross_border(self, obj):
        pass
    @abstractmethod
    def clean_up(self):
        pass
class Rectangle_block:
    def __init__(self, text, target=None, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)
        self.text.set_width(7/10 * width)
        self.height = height
        if(self.text.get_height() > 0.6 * height):
            self.text.scale(0.6 * height / self.text.get_height())
        self.width = width
        self.text_color = text_color
        self.font = font
        self.pointer = Triangle(color=color,fill_color=color,fill_opacity=1).flip(LEFT).scale(0.1)
        self.pointer.next_to(self.shape, TOP, 0.01)
        if target:
            self.owner = target
            self.all.scale(max(target.empty.get_height() / self.shape.get_height(), target.empty.get_width() / self.shape.get_width()))
    def replace_text(self, new_text, color=None):
        if not color:
            color = self.text_color
        new_text_obj = Text(new_text, color=color, font=self.font)
        new_text_obj.set_width(self.width * 7/10)
        if(new_text_obj.get_height() > 0.6 * self.height):
            new_text_obj.scale(0.6 * self.height / new_text_obj.get_height())
        return (Transform(self.text, new_text_obj.move_to(self.all.get_center())))
    def clean_up(self):
        return FadeOut(self.all)
class Stack(DataStructure, ABC):
    def __init__(self, ul, ur, ll, lr, aligned_edge, color=WHITE, text_color=WHITE, text_weight=NORMAL,font="Times New Roman"):
        super().__init__(ul, ur, ll, lr, aligned_edge, color, text_color, text_weight, font)
        self.empty = None
    def create_init(self, text=None, creation_style=None):
        if not creation_style:
            creation_style = "ShowCreation"
        empty = Init_structure(text, 0, self.max_width - 2 * MED_SMALL_BUFF, color=self.color, text_color=self.text_color)
        self.empty = empty.all
        empty.all.move_to(np.array([self.width_center, self.lr[1], 0]), aligned_edge=self.aligned_edge)
        self.all.add(empty.all)
        creation_transform = globals()[creation_style]
        return [creation_transform(empty.text), ShowCreation(empty.shape)]
    def push(self, obj, creation_style=None):
        if not creation_style:
            creation_style = "FadeIn"
        animations = []
        obj.all.move_to(np.array([self.width_center, self.ul[1] - 0.1, 0]), UP)
        shrink, scale_factor = self.shrink_if_cross_border(obj.all)
        if shrink:
            animations.append([shrink])
        target_width = self.all.get_width() * (scale_factor if scale_factor else 1)
        obj.all.scale(target_width / obj.all.get_width())
        creation_transform = globals()[creation_style]
        animations.append([creation_transform(obj.all)])
        animations.append([ApplyMethod(obj.all.next_to, self.all, np.array([0, 0.25, 0]))])
        return animations
    def pop(self, obj, fade_out=True):
        self.all.remove(obj.all)
        animation = [[ApplyMethod(obj.all.move_to, np.array([self.width_center, self.ul[1] - 0.1, 0]), UP)]]
        if fade_out:
            animation.append([FadeOut(obj.all)])
            enlarge, scale_factor = self.shrink(new_width=self.all.get_width(), new_height=self.all.get_height() + 0.25)
            if enlarge:
                animation.append([enlarge])
        return animation
    def shrink_if_cross_border(self, new_obj):
        height = new_obj.get_height()
        if self.will_cross_boundary(height, "TOP"):
            return self.shrink(new_width=self.all.get_width(), new_height=self.all.get_height() + height + 0.4)
        return 0, 1
    def push_existing(self, obj):
        animation = [[ApplyMethod(obj.all.move_to, np.array([self.width_center, self.ul[1] - 0.1, 0]), UP)]]
        enlarge, scale_factor = obj.owner.shrink(new_width=obj.owner.all.get_width(), new_height=obj.owner.all.get_height() + 0.25)
        sim_list = list()
        if enlarge:
            sim_list.append(enlarge)
        scale_factor = self.all.get_width() / obj.all.get_width()
        if scale_factor != 1:
            sim_list.append(ApplyMethod(obj.all.scale, scale_factor, {"about_edge": UP}))
        if len(sim_list) != 0:
            animation.append(sim_list)
        animation.append([ApplyMethod(obj.all.next_to, self.all, np.array([0, 0.25, 0]))])
        return animation
    def clean_up(self):
        return [FadeOut(self.all)]
# Object representing a stack instantiation.
class Init_structure:
    def __init__(self, text, angle, length=1.5, color=WHITE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.shape = Line(color=color)
        self.shape.set_length(length)
        self.shape.set_angle(angle)
        if text is not None:
            self.text = Text(text, color=text_color, weight=text_weight, font=font)
            self.text.next_to(self.shape, DOWN, SMALL_BUFF)
            self.all = VGroup(self.text, self.shape)
        else:
            self.all = VGroup(self.shape)