class DataStructure(ABC):
    def __init__(self, ul, ur, ll, lr, aligned_edge, color=WHITE, text_color=WHITE, text_weight=NORMAL,
                 font="Times New Roman"):
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
        scale_factor = min((self.max_width - 2 * MED_SMALL_BUFF) / new_width,
                           (self.max_height - 2 * MED_SMALL_BUFF) / new_height)
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
